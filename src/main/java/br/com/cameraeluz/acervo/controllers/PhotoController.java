package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.dto.PhotoUpdateDTO;
import br.com.cameraeluz.acervo.services.FileStorageService;
import br.com.cameraeluz.acervo.services.PhotoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
    private final PhotoService photoService; // Adicionado para resolver o erro de chamada

    @GetMapping("/view/**")
    public ResponseEntity<Resource> viewPhoto(HttpServletRequest request) {
        String path = extractPath(request, "/api/photos/view/");
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
}