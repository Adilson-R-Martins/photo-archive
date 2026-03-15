package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
import br.com.cameraeluz.acervo.dto.PhotoEventTrackResponseDTO;
import br.com.cameraeluz.acervo.services.PhotoEventTrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing photo participation records in events.
 *
 * <p>Allows ADMIN and EDITOR roles to register a photo in an event with a result
 * and any honors received. Read endpoints are accessible to all authenticated users.</p>
 */
@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class PhotoEventTrackController {

    private final PhotoEventTrackService trackService;

    /**
     * Registers a photo's participation in an event.
     *
     * @param request the participation details (photo id, event id, result type, honor, notes).
     * @return the created participation record as a {@link PhotoEventTrackResponseDTO}.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<PhotoEventTrackResponseDTO> createTrack(
            @Valid @RequestBody PhotoEventTrackRequestDTO request) {
        return ResponseEntity.ok(trackService.createTrack(request));
    }

    /**
     * Returns the full event history of a specific photo.
     *
     * @param photoId the id of the photo whose participation history is requested.
     * @return a list of participation records for the given photo.
     */
    @GetMapping("/photo/{photoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR', 'GUEST')")
    public ResponseEntity<List<PhotoEventTrackResponseDTO>> getTracksByPhoto(
            @PathVariable Long photoId) {
        return ResponseEntity.ok(trackService.findByPhoto(photoId));
    }

    /**
     * Returns all photo participations registered for a specific event.
     *
     * @param eventId the id of the event whose photo entries are requested.
     * @return a list of participation records for the given event.
     */
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR', 'GUEST')")
    public ResponseEntity<List<PhotoEventTrackResponseDTO>> getTracksByEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(trackService.findByEvent(eventId));
    }
}
