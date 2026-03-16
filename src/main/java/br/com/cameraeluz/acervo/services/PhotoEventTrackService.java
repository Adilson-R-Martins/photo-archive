package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
import br.com.cameraeluz.acervo.dto.PhotoEventTrackResponseDTO;
import br.com.cameraeluz.acervo.models.Event;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import br.com.cameraeluz.acervo.models.ResultType;
import br.com.cameraeluz.acervo.repositories.EventRepository;
import br.com.cameraeluz.acervo.repositories.PhotoEventTrackRepository;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.repositories.ResultTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing photo participation records in events.
 *
 * <p>Each record links a {@link Photo} to an {@link Event} with an associated
 * {@link ResultType}, optional honor description, and free-text notes.
 * Only active photos may be registered in events.</p>
 *
 * <h2>Visibility enforcement</h2>
 * <p>Both read methods ({@link #findByPhoto} and {@link #findByEvent}) enforce the
 * photo visibility policy via {@link PhotoService#isVisibleTo} to prevent IDOR:
 * without this guard, a caller could pass any {@code photoId} or {@code eventId}
 * and receive event participation history for photos they are not permitted to see —
 * including title, event name, result type, and honors (OWASP API1:2023).</p>
 *
 * <ul>
 *   <li>{@link #findByPhoto} — if the referenced photo is not visible to the caller,
 *       the method throws {@link AccessDeniedException} (→ 403). The photo's existence
 *       is known at this point (the caller passed a specific id), so a 403 is more
 *       informative than 404 for authenticated callers.</li>
 *   <li>{@link #findByEvent} — PRIVATE photos that the caller cannot see are
 *       silently omitted from the returned list. This avoids revealing how many private
 *       photos participated in an event, consistent with the listing endpoints.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PhotoEventTrackService {

    private final PhotoEventTrackRepository trackRepository;
    private final PhotoRepository photoRepository;
    private final EventRepository eventRepository;
    private final ResultTypeRepository resultTypeRepository;
    private final PhotoService photoService;

    /**
     * Creates a new participation record linking a photo to an event.
     *
     * @param request the DTO containing the photo id, event id, result type id,
     *                optional honor, and optional notes.
     * @return the persisted record as a {@link PhotoEventTrackResponseDTO}.
     * @throws EntityNotFoundException if the photo, event, or result type is not found.
     * @throws IllegalStateException   if the photo is inactive and therefore ineligible
     *                                 for event registration.
     */
    @Transactional
    public PhotoEventTrackResponseDTO createTrack(PhotoEventTrackRequestDTO request) {
        Photo photo = photoRepository.findById(request.getPhotoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo with id " + request.getPhotoId() + " was not found."));

        if (!photo.isActive()) {
            throw new IllegalStateException(
                    "Photo with id " + request.getPhotoId() + " is inactive and cannot be registered in events. "
                    + "Reactivate the photo before creating a participation record.");
        }

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Event with id " + request.getEventId() + " was not found."));

        ResultType resultType = resultTypeRepository.findById(request.getResultTypeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Result type with id " + request.getResultTypeId() + " was not found."));

        PhotoEventTrack track = new PhotoEventTrack();
        track.setPhoto(photo);
        track.setEvent(event);
        track.setResultType(resultType);
        track.setHonorReceived(request.getHonor());
        track.setNotes(request.getNotes());

        return toResponseDTO(trackRepository.save(track));
    }

    /**
     * Returns all participation records for a specific photo, subject to the
     * caller's visibility permissions.
     *
     * <p>If the photo exists but is not visible to the caller (e.g., it is
     * {@link br.com.cameraeluz.acervo.models.enums.Visibility#PRIVATE} and the
     * caller does not hold an active permission), an {@link AccessDeniedException}
     * is thrown. This prevents exposing event history — including event name, result
     * type, and honors — for photos the caller is not authorised to see.</p>
     *
     * @param photoId        the id of the photo whose event history is requested.
     * @param authentication the caller's authentication context.
     * @return a list of {@link PhotoEventTrackResponseDTO}; empty if none exist.
     * @throws EntityNotFoundException if no photo with the given id exists.
     * @throws AccessDeniedException   if the photo is not visible to the caller.
     */
    @Transactional(readOnly = true)
    public List<PhotoEventTrackResponseDTO> findByPhoto(Long photoId, Authentication authentication) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo with id " + photoId + " was not found."));

        // Guard: reject callers who cannot see the photo. Without this check, the
        // track list would expose event participation details (event name, result,
        // honor) for PRIVATE photos — an IDOR vulnerability (OWASP API1:2023).
        if (!photoService.isVisibleTo(photo, authentication)) {
            throw new AccessDeniedException(
                    "You do not have permission to view the event history of this photo.");
        }

        return trackRepository.findByPhoto(photo).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns the participation records for a specific event that are visible to
     * the caller.
     *
     * <p>Tracks whose linked photo is not visible to the caller are silently omitted.
     * This approach avoids leaking how many private photos participated in an event,
     * consistent with the behaviour of the listing endpoints.</p>
     *
     * <p>The visibility check runs within the same read-only transaction opened by
     * this method, so lazy-loaded associations on the {@link Photo} entity (such as
     * {@code uploadedBy}) are accessible without a separate database round-trip per
     * track.</p>
     *
     * @param eventId        the id of the event whose photo entries are requested.
     * @param authentication the caller's authentication context.
     * @return a filtered list of {@link PhotoEventTrackResponseDTO} for photos the
     *         caller is permitted to see; empty if none exist or all are filtered out.
     * @throws EntityNotFoundException if no event with the given id exists.
     */
    @Transactional(readOnly = true)
    public List<PhotoEventTrackResponseDTO> findByEvent(Long eventId, Authentication authentication) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Event with id " + eventId + " was not found."));

        return trackRepository.findByEvent(event).stream()
                // Silently omit tracks whose photo is not visible to the caller.
                // PRIVATE photos for which the caller lacks permission are excluded
                // without a 403/404, so the total count of event entries is not leaked.
                .filter(track -> photoService.isVisibleTo(track.getPhoto(), authentication))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private PhotoEventTrackResponseDTO toResponseDTO(PhotoEventTrack track) {
        PhotoEventTrackResponseDTO dto = new PhotoEventTrackResponseDTO();
        dto.setId(track.getId());
        dto.setPhotoId(track.getPhoto().getId());
        dto.setPhotoTitle(track.getPhoto().getTitle());
        dto.setEventId(track.getEvent().getId());
        dto.setEventName(track.getEvent().getName());
        dto.setEventType(track.getEvent().getType());
        dto.setEventDate(track.getEvent().getEventDate());
        dto.setResultTypeId(track.getResultType().getId());
        dto.setResultDescription(track.getResultType().getDescription());
        dto.setHonorReceived(track.getHonorReceived());
        dto.setNotes(track.getNotes());
        dto.setRegisteredAt(track.getRegisteredAt());
        return dto;
    }
}
