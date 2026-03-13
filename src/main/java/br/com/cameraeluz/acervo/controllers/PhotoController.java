package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final FileStorageService fileStorageService;

    /**
     * Serve a imagem para exibição direta no navegador (Ex: <img src="...">)
     * O mapeamento /** permite capturar caminhos como 2024/11/thumbnails/foto.jpg
     */
    @GetMapping("/view/**")
    public ResponseEntity<Resource> viewPhoto(HttpServletRequest request) {
        String path = extractPath(request, "/api/photos/view/");
        Resource resource = fileStorageService.loadFileAsResource(path);

        String contentType = determineContentType(resource);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline") // "inline" abre no navegador
                .body(resource);
    }

    /**
     * Força o download do arquivo original
     */
    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadPhoto(HttpServletRequest request) {
        String path = extractPath(request, "/api/photos/download/");
        Resource resource = fileStorageService.loadFileAsResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // Metodo auxiliar para extrair o caminho completo após o prefixo do endpoint
    private String extractPath(HttpServletRequest request, String prefix) {
        String fullPath = request.getRequestURI();
        return fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
    }

    // Metodo auxiliar para detectar se é JPG ou PNG dinamicamente
    private String determineContentType(Resource resource) {
        try {
            return Files.probeContentType(resource.getFile().toPath());
        } catch (IOException e) {
            return "image/jpeg"; // fallback default
        }
    }
}