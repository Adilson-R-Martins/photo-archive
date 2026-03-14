package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import br.com.cameraeluz.acervo.services.PhotoEventTrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor // <-- Substitui todos os seus @Autowired!
public class PhotoEventTrackController {

    private final PhotoEventTrackService trackService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<PhotoEventTrack> createTrack(
            @Valid @RequestBody PhotoEventTrackRequestDTO request) {
        return ResponseEntity.ok(trackService.createTrack(request));
    }

    /**
     * Returns the full event history of a specific photo.
     * Accessible by any authenticated user.
     */
    @GetMapping("/photo/{photoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR', 'GUEST')")
    public ResponseEntity<List<PhotoEventTrack>> getTracksByPhoto(
            @PathVariable Long photoId) {
        return ResponseEntity.ok(trackService.findByPhoto(photoId));
    }

    /**
     * Returns all photo participations registered for a specific event.
     * Accessible by any authenticated user.
     */
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR', 'GUEST')")
    public ResponseEntity<List<PhotoEventTrack>> getTracksByEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(trackService.findByEvent(eventId));
    }
}