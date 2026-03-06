package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.models.Category;
import br.com.cameraeluz.acervo.models.ExifData;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.repositories.CategoryRepository;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import br.com.cameraeluz.acervo.repositories.specs.PhotoSpecifications;
import br.com.cameraeluz.acervo.services.FileStorageService;
import br.com.cameraeluz.acervo.services.ImageService;
import br.com.cameraeluz.acervo.services.MetadataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Controller for managing high-level Photo entities.
 * Orchestrates file storage, metadata extraction, and database persistence.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ImageService imageService;

    /**
     * Handles the upload of a new photograph.
     * This process includes physical storage, automatic EXIF extraction, triggers automatic web optimization. and DB mapping.
     * * @param file The image file (Multipart)
     * @param title Title of the work
     * @param artisticAuthorName Name of the artist/photographer
     * @param categoryIds List of IDs for Many-to-Many relationship with Categories
     * @return ResponseEntity with the saved Photo entity
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<?> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("artisticAuthorName") String artisticAuthorName,
            @RequestParam("categoryIds") List<Long> categoryIds) {

        try {
            // 0. Security Check: Validate if the file is actually an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body("Security Error: Only image files (JPEG, PNG, etc.) are allowed.");
            }

            // 1. Resolve Auth User (uploadedBy)
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Error: Current user not found."));

            // 2. Resolve Multiple Categories (ManyToMany)
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));

            // 3. Extract technical metadata automatically via MetadataService
            ExifData extractedExif = metadataService.extractMetadata(file);

            // 4. Store Physical File and get the unique storage name
            String storageName = fileStorageService.storeFile(file);

            // 5. Generate Web-Optimized Version (Automatic Resizing)
            String optimizedName = imageService.generateWebOptimizedVersion(storageName);

            // 6. Build Photo Object based on your existing professional model
            Photo photo = new Photo();
            photo.setTitle(title);
            photo.setArtisticAuthorName(artisticAuthorName);
            photo.setUploadedBy(currentUser);
            photo.setCategories(categories);
            photo.setExifData(extractedExif);
            photo.setOriginalFileName(file.getOriginalFilename());
            photo.setStoragePath(storageName);
            photo.setWebOptimizedPath(optimizedName);
            photo.setCreatedAt(LocalDateTime.now());

            return ResponseEntity.ok(photoRepository.save(photo));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error during upload process: " + e.getMessage());
        }
    }

    /**
     * Streams the web-optimized version of an image for browser display.
     */
    @GetMapping("/view/{fileName:.+}")
    public ResponseEntity<Resource> viewPhoto(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName, true);
        return getResourceResponseEntity(resource, request, "inline");
    }

    /**
     * Downloads the original high-resolution file.
     */
    @GetMapping("/download/{fileName:.+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')") // Example: Restrict high-res downloads
    public ResponseEntity<Resource> downloadPhoto(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName, false);
        return getResourceResponseEntity(resource, request, "attachment");
    }

    private ResponseEntity<Resource> getResourceResponseEntity(Resource resource, HttpServletRequest request, String contentDisposition) {
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition + "; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Unified search endpoint for the photo archive.
     * Replaces the old getPhotos to support advanced filtering (events, awards, and metadata).
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<PhotoResponseDTO>> searchPhotos(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long resultTypeId,
            @RequestParam(required = false) String keyword) {

        // Uses the new Specification that handles all filters simultaneously
        Specification<Photo> spec = PhotoSpecifications.withAdvancedFilters(
                authorId, eventId, resultTypeId, keyword);

        List<PhotoResponseDTO> response = photoRepository.findAll(spec).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private PhotoResponseDTO convertToDTO(Photo photo) {
        // Generate full URLs for the frontend
        String viewUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/view/").path(photo.getWebOptimizedPath()).toUriString();

        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/download/").path(photo.getStoragePath()).toUriString();

        PhotoResponseDTO dto = new PhotoResponseDTO();
        dto.setId(photo.getId());
        dto.setTitle(photo.getTitle());
        dto.setArtisticAuthorName(photo.getArtisticAuthorName());
        dto.setViewUrl(viewUrl);
        dto.setDownloadUrl(downloadUrl);
        dto.setMetadata(photo.getExifData());
        dto.setCategories(photo.getCategories().stream().map(c -> c.getName()).collect(Collectors.toSet()));

        // Map the traceability history
        if (photo.getEventTracks() != null) {
            dto.setEventHistory(photo.getEventTracks().stream().map(track -> {
                PhotoResponseDTO.TrackInfoDTO t = new PhotoResponseDTO.TrackInfoDTO();
                t.setEventName(track.getEvent().getName());
                t.setResultDescription(track.getResultType().getDescription());
                t.setHonorReceived(track.getHonor());
                t.setEventDate(track.getEvent().getEventDate().toString());
                return t;
            }).collect(Collectors.toList()));
        }

        return dto;
    }
}