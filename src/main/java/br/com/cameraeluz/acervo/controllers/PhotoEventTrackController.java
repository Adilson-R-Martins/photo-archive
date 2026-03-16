package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
import br.com.cameraeluz.acervo.dto.PhotoEventTrackResponseDTO;
import br.com.cameraeluz.acervo.services.PhotoEventTrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing photo participation records in events.
 *
 * <p>Allows EDITOR and ADMIN roles to register a photo in an event with a result
 * and any honors received. Read endpoints are accessible to all authenticated users
 * but are scoped to photos the caller is permitted to see.</p>
 *
 * <h2>IDOR prevention</h2>
 * <p>The {@link Authentication} context is forwarded to the service layer for all
 * read operations. {@link PhotoEventTrackService} applies the photo visibility policy
 * before returning any event history, preventing callers from discovering participation
 * data for PRIVATE photos they do not have access to.</p>
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
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<PhotoEventTrackResponseDTO> createTrack(
            @Valid @RequestBody PhotoEventTrackRequestDTO request) {
        return ResponseEntity.ok(trackService.createTrack(request));
    }

    /**
     * Returns the full event history of a specific photo, subject to visibility.
     *
     * <p>Returns {@code 403 Forbidden} if the photo exists but is not visible to
     * the caller (e.g., PRIVATE photo without an active DownloadPermission).</p>
     *
     * @param photoId        the id of the photo whose participation history is requested.
     * @param authentication the caller's authentication context.
     * @return a list of participation records for the given photo.
     */
    @GetMapping("/photo/{photoId}")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<List<PhotoEventTrackResponseDTO>> getTracksByPhoto(
            @PathVariable Long photoId,
            Authentication authentication) {
        return ResponseEntity.ok(trackService.findByPhoto(photoId, authentication));
    }

    /**
     * Returns all photo participations for a specific event that are visible to
     * the caller.
     *
     * <p>Tracks for PRIVATE photos that the caller cannot see are silently omitted
     * from the response. The total count of event participants is therefore not leaked
     * to unauthorized callers.</p>
     *
     * @param eventId        the id of the event whose photo entries are requested.
     * @param authentication the caller's authentication context.
     * @return a filtered list of participation records for the given event.
     */
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<List<PhotoEventTrackResponseDTO>> getTracksByEvent(
            @PathVariable Long eventId,
            Authentication authentication) {
        return ResponseEntity.ok(trackService.findByEvent(eventId, authentication));
    }
}
