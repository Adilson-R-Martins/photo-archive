package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.dto.PhotoUpdateDTO;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.enums.Visibility;
import br.com.cameraeluz.acervo.security.SecurityUtils;
import br.com.cameraeluz.acervo.services.DownloadPermissionService;
import br.com.cameraeluz.acervo.services.FileStorageService;
import br.com.cameraeluz.acervo.services.PhotoService;
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
 * <h2>Access control layers</h2>
 * <ol>
 *   <li><strong>Filter chain</strong> — coarse authentication requirements
 *       (e.g., {@code /api/photos/view/**} is {@code permitAll}; all other
 *       read endpoints require a valid JWT).</li>
 *   <li><strong>Method-level {@code @PreAuthorize}</strong> — role checks for
 *       write operations (upload, update, delete).</li>
 *   <li><strong>Per-photo visibility</strong> — {@link PhotoService#isVisibleTo}
 *       enforces the {@link Visibility} tier on single-photo read endpoints
 *       ({@code /view/**} and {@code /{id}}); the listing/search endpoints
 *       are filtered at the query layer via
 *       {@link br.com.cameraeluz.acervo.repositories.specs.PhotoSpecifications}.</li>
 *   <li><strong>{@link DownloadPermissionService}</strong> — fine-grained
 *       per-user download permission with atomic counter tracking.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final FileStorageService fileStorageService;
    private final PhotoService photoService;
    private final DownloadPermissionService downloadPermissionService;

    /**
     * Returns a paginated list of active photos visible to the caller, ordered
     * by most recent first.
     *
     * <p>Equivalent to calling {@code GET /api/photos/search} with no filters.
     * Results are scoped to the caller's {@link Visibility} tier:
     * ADMIN/EDITOR see all; regular users see OPEN, PUBLIC, and their own PRIVATE photos.</p>
     *
     * @param pageable       pagination and sorting parameters.
     * @param authentication the caller's authentication context, injected by Spring MVC.
     * @return a paginated list of visible active photos as {@link PhotoResponseDTO}.
     */
    @GetMapping
    public ResponseEntity<Page<PhotoResponseDTO>> listPhotos(
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(
                photoService.searchPhotos(null, null, null, null, pageable, authentication));
    }

    /**
     * Returns the full detail of a single active photo by its id.
     *
     * <p>The photo's {@link Visibility} is checked against the caller's context:</p>
     * <ul>
     *   <li>OPEN / PUBLIC — any authenticated user may access.</li>
     *   <li>PRIVATE — only the uploader, ADMIN, and EDITOR may access.</li>
     * </ul>
     * <p>Returns {@code 403 Forbidden} if the caller does not meet the visibility
     * requirements for the requested photo.</p>
     *
     * @param id             the photo id.
     * @param authentication the caller's authentication context.
     * @return the photo as a {@link PhotoResponseDTO}.
     * @throws jakarta.persistence.EntityNotFoundException if no photo with the given id exists.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PhotoResponseDTO> getPhoto(
            @PathVariable Long id,
            Authentication authentication) {
        Photo photo = photoService.findById(id);
        if (!photoService.isVisibleTo(photo, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(photoService.convertToDTO(photo));
    }

    /**
     * Serves the web-optimised version of a photo inline (for browser display).
     *
     * <p>This endpoint is open at the filter-chain level ({@code permitAll}) so that
     * {@link Visibility#OPEN} photos are accessible without authentication. Visibility
     * is enforced here at the application layer:</p>
     * <ul>
     *   <li>OPEN — served to anyone.</li>
     *   <li>PUBLIC — served to authenticated users; unauthenticated callers receive
     *       {@code 404} (avoids leaking the existence of the resource).</li>
     *   <li>PRIVATE — served only to the uploader, ADMIN, and EDITOR;
     *       all other callers receive {@code 404}.</li>
     * </ul>
     * <p>Returns {@code 410 Gone} if the photo has been soft-deleted.</p>
     *
     * @param request        the current HTTP request, used to extract the path suffix.
     * @param authentication the caller's authentication context; may be {@code null}
     *                       or anonymous for unauthenticated requests.
     * @return the image resource with an appropriate {@code Content-Type} header,
     *         or an error response if the photo is deleted or not accessible.
     * @throws jakarta.persistence.EntityNotFoundException if no photo matches the path.
     */
    @GetMapping("/view/**")
    public ResponseEntity<Resource> viewPhoto(
            HttpServletRequest request,
            Authentication authentication) {

        String path = extractPath(request, "/api/photos/view/");

        Photo photo = photoService.findByWebOptimizedPath(path);

        if (!photo.isActive()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        // Enforce per-photo visibility.
        // 404 is intentional: returning 401 or 403 would reveal that a photo
        // exists at this path but is not accessible to the caller.
        if (!photoService.isVisibleTo(photo, authentication)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = fileStorageService.loadFileAsResource(path);
        String contentType = determineContentType(resource);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    /**
     * Downloads the original high-resolution file for a photo.
     *
     * <p>Authorization is delegated to {@link DownloadPermissionService#canDownload},
     * which enforces the full precedence chain (ADMIN/EDITOR &gt; owner &gt; permission record).
     * The download counter is atomically incremented inside that call when a permission
     * record is consumed.</p>
     *
     * <p>Note: download access is <em>orthogonal</em> to photo visibility. An
     * {@link Visibility#OPEN} photo still requires an explicit
     * {@link br.com.cameraeluz.acervo.models.DownloadPermission} for its original file.</p>
     *
     * @param photoId        the id of the photo to download.
     * @param authentication the caller's authentication object.
     * @return the original file as an attachment.
     * @throws jakarta.persistence.EntityNotFoundException if the photo does not exist or the
     *                                 caller's permission record is not found.
     */
    @GetMapping("/download/{photoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadPhoto(
            @PathVariable Long photoId,
            Authentication authentication) {

        Photo photo = photoService.findById(photoId);

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

        // Sanitize the filename before embedding it in the Content-Disposition header.
        // Raw filenames stored in the DB may contain quotes or CRLF sequences that could
        // inject additional HTTP response headers or corrupt the header value.
        String rawFilename = photo.getOriginalFileName() != null
                ? photo.getOriginalFileName()
                : resource.getFilename();
        String safeFilename = (rawFilename != null ? rawFilename : "photo")
                .replaceAll("[^a-zA-Z0-9._\\- ]", "_");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFilename + "\"")
                .body(resource);
    }

    /**
     * Uploads a new photo to the archive.
     *
     * <p>Accepts a multipart form with the image file, display title, optional
     * artistic author name override, optional initial visibility, and one or
     * more category ids.</p>
     *
     * <p>If {@code visibility} is omitted, the photo defaults to
     * {@link Visibility#PRIVATE}, following the Principle of Least Privilege.
     * The author must explicitly publish the photo to make it accessible to others.</p>
     *
     * @param file               the image file (JPEG, PNG, TIFF, or WebP).
     * @param title              the display title for the photo.
     * @param artisticAuthorName optional override for the artistic author name;
     *                           falls back to the uploader's profile artistic name.
     * @param visibility         initial access policy; defaults to {@link Visibility#PRIVATE}.
     * @param categoryIds        the set of category ids to associate with the photo.
     * @return {@code 200 OK} with the persisted photo as a {@link PhotoResponseDTO}.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<PhotoResponseDTO> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "artisticAuthorName", required = false) String artisticAuthorName,
            @RequestParam(value = "visibility", defaultValue = "PRIVATE") Visibility visibility,
            @RequestParam("categories") Set<Long> categoryIds) {

        return ResponseEntity.ok(
                photoService.uploadPhoto(file, title, artisticAuthorName, visibility, categoryIds));
    }

    /**
     * Updates mutable fields of an existing photo.
     *
     * <p>The transport layer allows AUTHOR+ (filter chain). Fine-grained ownership
     * verification — ADMIN/EDITOR may edit any photo; AUTHOR may only edit their own —
     * is enforced inside {@link PhotoService#updatePhoto} via a single entity load.
     * This eliminates the previous double-query TOCTOU pattern where
     * {@code @photoService.isOwner()} and the service method each called
     * {@code findById} independently.</p>
     *
     * <p>All fields in {@link PhotoUpdateDTO} are optional — only non-null values are
     * applied. This includes {@link Visibility}: set it to promote or restrict
     * access without affecting other fields.</p>
     *
     * @param id             the id of the photo to update.
     * @param dto            the partial update payload.
     * @param authentication the caller's authentication context.
     * @return the updated photo as a {@link PhotoResponseDTO}.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<PhotoResponseDTO> updatePhoto(
            @PathVariable Long id,
            @Valid @RequestBody PhotoUpdateDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(photoService.updatePhoto(id, dto, authentication));
    }

    /**
     * Soft-deletes a photo by setting its {@code active} flag to {@code false}.
     *
     * <p>The transport layer allows AUTHOR+. Ownership is verified inside
     * {@link PhotoService#deletePhoto} — ADMIN/EDITOR may delete any photo;
     * AUTHOR may only delete their own.</p>
     *
     * <p>Soft-deleted photos are hidden from all queries and their view URL
     * returns {@code 410 Gone}.</p>
     *
     * @param id             the id of the photo to deactivate.
     * @param authentication the caller's authentication context.
     * @return {@code 204 No Content} on success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long id,
            Authentication authentication) {
        photoService.deletePhoto(id, authentication);
        return ResponseEntity.noContent().build();
    }

    /**
     * Searches photos with optional filters, pagination, and visibility scoping.
     *
     * <p>Example: {@code GET /api/photos/search?keyword=bird&page=0&size=20&sort=createdAt,desc}</p>
     *
     * <p>Results are scoped to photos the caller is permitted to see. ADMIN/EDITOR
     * receive all matching photos; regular users see OPEN, PUBLIC, and their own
     * PRIVATE photos.</p>
     *
     * @param authorId       optional filter by uploader user id.
     * @param eventId        optional filter by event id.
     * @param resultTypeId   optional filter by result type id.
     * @param keyword        optional free-text keyword searched across title and EXIF/IPTC fields.
     * @param pageable       pagination and sorting parameters.
     * @param authentication the caller's authentication context.
     * @return a paginated list of matching, visible photos as {@link PhotoResponseDTO}.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PhotoResponseDTO>> searchPhotos(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long resultTypeId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        return ResponseEntity.ok(
                photoService.searchPhotos(authorId, eventId, resultTypeId, keyword, pageable, authentication));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

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
}
