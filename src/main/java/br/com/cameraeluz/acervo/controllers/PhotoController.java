package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.models.*;
import br.com.cameraeluz.acervo.repositories.*;
import br.com.cameraeluz.acervo.repositories.specs.PhotoSpecifications;
import br.com.cameraeluz.acervo.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    @Autowired private PhotoRepository photoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ImageService imageService;
    @Autowired private MetadataService metadataService;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<PhotoResponseDTO>> searchPhotos(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long resultTypeId,
            @RequestParam(required = false) String keyword) {

        Specification<Photo> spec = PhotoSpecifications.withAdvancedFilters(authorId, eventId, resultTypeId, keyword);
        List<PhotoResponseDTO> response = photoRepository.findAll(spec).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("artisticAuthorName") String artisticAuthorName) {

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Agora com 2 argumentos conforme a nova Interface
            String originalFileName = fileStorageService.storeFile(file, title);

            // Verifique se o nome no seu ImageService é generateWebVersion ou generateWebOptimizedVersion
            String webOptimizedName = imageService.generateWebOptimizedVersion(originalFileName);

            Photo photo = new Photo();
            photo.setTitle(title);
            photo.setArtisticAuthorName(artisticAuthorName);
            photo.setUploadedBy(user);
            photo.setStoragePath(originalFileName);
            photo.setWebOptimizedPath(webOptimizedName);
            photo.setOriginalFileName(file.getOriginalFilename());
            photo.setExifData(metadataService.extractMetadata(file));

            return ResponseEntity.ok(photoRepository.save(photo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        String contentType = "application/octet-stream";
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) { }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private PhotoResponseDTO convertToDTO(Photo photo) {
        PhotoResponseDTO dto = new PhotoResponseDTO();
        dto.setId(photo.getId());
        dto.setTitle(photo.getTitle());
        dto.setArtisticAuthorName(photo.getArtisticAuthorName());
        dto.setMetadata(photo.getExifData());

        dto.setViewUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/view/").path(photo.getWebOptimizedPath()).toUriString());

        dto.setDownloadUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/download/").path(photo.getStoragePath()).toUriString());

        dto.setCategories(photo.getCategories().stream().map(Category::getName).collect(Collectors.toSet()));

        if (photo.getEventTracks() != null) {
            dto.setEventHistory(photo.getEventTracks().stream().map(track -> {
                PhotoResponseDTO.TrackInfoDTO t = new PhotoResponseDTO.TrackInfoDTO();
                t.setEventName(track.getEvent().getName());
                t.setResultDescription(track.getResultType().getDescription());
                // Ajustado para o nome do campo na sua Entity corrigida
                t.setHonorReceived(track.getHonorReceived());
                t.setEventDate(track.getEvent().getEventDate().toString());
                return t;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}