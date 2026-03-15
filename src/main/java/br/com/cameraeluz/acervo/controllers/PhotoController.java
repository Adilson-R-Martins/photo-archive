package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.dto.PhotoUpdateDTO;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.security.SecurityUtils;
import br.com.cameraeluz.acervo.services.DownloadPermissionService;
import br.com.cameraeluz.acervo.services.FileStorageService;
import br.com.cameraeluz.acervo.services.PhotoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

/**
 * REST controller exposing photo-related endpoints: view, download, upload,
 * update, delete, and search.
 *
 * <p>Access control is enforced at two levels: method-level {@code @PreAuthorize}
 * annotations for coarse-grained role checks, and {@link DownloadPermissionService}
 * for fine-grained download permission validation.</p>
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final FileStorageService fileStorageService;
    private final PhotoService photoService;
    private final PhotoRepository photoRepository;
    private final DownloadPermissionService downloadPermissionService;

    /**
     * Serves the web-optimised version of a photo inline (for browser display).
     *
     * <p>Returns {@code 410 Gone} if the photo exists but has been soft-deleted.
     * Visibility rules (PUBLIC vs. PRIVATE) are enforced by the security filter chain.</p>
     *
     * @param request the current HTTP request used to extract the path suffix.
     * @return the image resource with an appropriate {@code Content-Type} header.
     * @throws EntityNotFoundException if no photo matches the requested path.
     */
    @GetMapping("/view/**")
    public ResponseEntity<Resource> viewPhoto(HttpServletRequest request) {
        String path = extractPath(request, "/api/photos/view/");

        Photo photo = photoRepository.findByWebOptimizedPath(path)
                .orElseThrow(() -> new EntityNotFoundException("Photo was not found at the requested path."));

        if (!photo.isActive()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        Resource resource = fileStorageService.loadFileAsResource(path);
        String contentType = determineContentType(resource);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    /**
     * Extracts the path suffix that follows a fixed prefix from the request URI
     * and rejects any path containing traversal sequences.
     *
     * @param request the incoming HTTP request.
     * @param prefix  the leading URI segment to strip (e.g., {@code "/api/photos/view/"}).
     * @return the portion of the URI after the prefix.
     * @throws IllegalArgumentException if the extracted path contains {@code ..}.
     */
    private String extractPath(HttpServletRequest request, String prefix) {
        String fullPath = request.getRequestURI();
        String path = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
        if (path.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid photo path: traversal sequences are not permitted.");
        }
        return path;
    }

    private String determineContentType(Resource resource) {
        try {
            return Files.probeContentType(resource.getFile().toPath());
        } catch (IOException e) {
            return "image/jpeg";
        }
    }

    /**
     * Downloads the original high-resolution file for a photo.
     *
     * <p>Authorization is delegated to {@link DownloadPermissionService#canDownload},
     * which enforces the full precedence chain (ADMIN/EDITOR &gt; owner &gt; permission record).
     * The download counter is atomically incremented inside that call when a permission
     * record is consumed.</p>
     *
     * @param photoId        the id of the photo to download.
     * @param authentication the caller's authentication object.
     * @return the original file as an attachment.
     * @throws EntityNotFoundException if the photo does not exist or the caller's
     *                                 permission record is not found.
     */
    @GetMapping("/download/{photoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadPhoto(
            @PathVariable Long photoId,
            Authentication authentication) {

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Photo not found with id: " + photoId));

        if (!photo.isActive()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        // Throws DownloadRevokedException / DownloadLimitReachedException / EntityNotFoundException
        // if the caller is not authorized. Atomically increments the counter if applicable.
        downloadPermissionService.canDownload(
                SecurityUtils.getUserDetails(authentication).getId(),
                photoId,
                SecurityUtils.extractRoles(authentication));

        Resource resource = fileStorageService.loadFileAsResource(photo.getStoragePath());
        String contentType = determineContentType(resource);
        String filename = photo.getOriginalFileName() != null
                ? photo.getOriginalFileName()
                : resource.getFilename();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Uploads a new photo to the archive.
     *
     * <p>Accepts a multipart form with the image file, display title, optional
     * artistic author name override, and one or more category ids.</p>
     *
     * @param file               the image file (JPEG, PNG, TIFF, or WebP).
     * @param title              the display title for the photo.
     * @param artisticAuthorName optional override for the artistic author name.
     * @param categoryIds        the set of category ids to associate with the photo.
     * @return the persisted photo as a {@link PhotoResponseDTO}.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR')")
    public ResponseEntity<PhotoResponseDTO> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "artisticAuthorName", required = false) String artisticAuthorName,
            @RequestParam("categories") Set<Long> categoryIds) {

        return ResponseEntity.ok(photoService.uploadPhoto(file, title, artisticAuthorName, categoryIds));
    }

    /**
     * Updates mutable fields of an existing photo.
     *
     * <p>Callers must be ADMIN, EDITOR, or the photo's original uploader.</p>
     *
     * @param id  the id of the photo to update.
     * @param dto the partial update payload.
     * @return the updated photo as a {@link PhotoResponseDTO}.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR') or @photoService.isOwner(authentication.name, #id)")
    public ResponseEntity<PhotoResponseDTO> updatePhoto(
            @PathVariable Long id,
            @Valid @RequestBody PhotoUpdateDTO dto) {
        return ResponseEntity.ok(photoService.updatePhoto(id, dto));
    }

    /**
     * Soft-deletes a photo by setting its {@code active} flag to {@code false}.
     *
     * <p>Callers must be ADMIN, EDITOR, or the photo's original uploader.</p>
     *
     * @param id the id of the photo to deactivate.
     * @return {@code 204 No Content} on success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR') or @photoService.isOwner(authentication.name, #id)")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        PhotoUpdateDTO softDeleteDto = new PhotoUpdateDTO();
        softDeleteDto.setActive(false);
        photoService.updatePhoto(id, softDeleteDto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Searches photos with optional filters and pagination.
     *
     * <p>Example: {@code GET /api/photos/search?keyword=bird&page=0&size=20&sort=createdAt,desc}</p>
     *
     * @param authorId     optional filter by uploader user id.
     * @param eventId      optional filter by event id.
     * @param resultTypeId optional filter by result type id.
     * @param keyword      optional free-text keyword searched across title and EXIF/IPTC fields.
     * @param pageable     pagination and sorting parameters.
     * @return a paginated list of matching photos as {@link PhotoResponseDTO}.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PhotoResponseDTO>> searchPhotos(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long resultTypeId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                photoService.searchPhotos(authorId, eventId, resultTypeId, keyword, pageable));
    }
}
