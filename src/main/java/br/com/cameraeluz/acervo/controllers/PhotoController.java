package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.dto.PhotoUpdateDTO;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final FileStorageService fileStorageService;
    private final PhotoService photoService;
    private final PhotoRepository photoRepository;// Adicionado para resolver o erro de chamada

    @GetMapping("/view/**")
    public ResponseEntity<Resource> viewPhoto(HttpServletRequest request) {
        String path = extractPath(request, "/api/photos/view/");

        // Verifica se a foto existe e está ativa antes de servir
        Photo photo = photoRepository.findByWebOptimizedPath(path)
                .orElseThrow(() -> new EntityNotFoundException("Foto não encontrada."));

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

    // Metodo auxiliar para extrair o caminho
    private String extractPath(HttpServletRequest request, String prefix) {
        String fullPath = request.getRequestURI();
        return fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
    }

    private String determineContentType(Resource resource) {
        try {
            return Files.probeContentType(resource.getFile().toPath());
        } catch (IOException e) {
            return "image/jpeg";
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR')")
    public ResponseEntity<PhotoResponseDTO> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "artisticAuthorName", required = false) String artisticAuthorName,
            @RequestParam("categories") Set<Long> categoryIds) {

        return ResponseEntity.ok(photoService.uploadPhoto(file, title, artisticAuthorName, categoryIds));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR') or @photoService.isOwner(authentication.name, #id)")
    public ResponseEntity<PhotoResponseDTO> updatePhoto(
            @PathVariable Long id,
            @Valid @RequestBody PhotoUpdateDTO dto) {
        return ResponseEntity.ok(photoService.updatePhoto(id, dto));
    }

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
     * <p>
     * Example: GET /api/photos/search?keyword=passaro&page=0&size=20&sort=createdAt,desc
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR', 'GUEST')")
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