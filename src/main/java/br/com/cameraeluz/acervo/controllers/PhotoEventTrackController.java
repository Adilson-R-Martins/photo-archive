package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import br.com.cameraeluz.acervo.services.PhotoEventTrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor // <-- Substitui todos os seus @Autowired!
public class PhotoEventTrackController {

    // Injetamos apenas o Service! O Controller não precisa mais falar com Repositories
    private final PhotoEventTrackService trackService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<PhotoEventTrack> createTrack(@Valid @RequestBody PhotoEventTrackRequestDTO request) {
        PhotoEventTrack createdTrack = trackService.createTrack(request);
        return ResponseEntity.ok(createdTrack);
    }
}