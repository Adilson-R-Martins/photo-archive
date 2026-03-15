package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing photo participation records in events.
 *
 * <p>Each record links a {@link Photo} to an {@link Event} with an associated
 * {@link ResultType}, optional honor description, and free-text notes.
 * Only active photos may be registered in events.</p>
 */
@Service
@RequiredArgsConstructor
public class PhotoEventTrackService {

    private final PhotoEventTrackRepository trackRepository;
    private final PhotoRepository photoRepository;
    private final EventRepository eventRepository;
    private final ResultTypeRepository resultTypeRepository;

    /**
     * Creates a new participation record linking a photo to an event.
     *
     * @param request the DTO containing the photo id, event id, result type id,
     *                optional honor, and optional notes.
     * @return the persisted {@link PhotoEventTrack} entity.
     * @throws EntityNotFoundException if the photo, event, or result type is not found.
     * @throws IllegalStateException   if the photo is inactive and therefore ineligible
     *                                 for event registration.
     */
    public PhotoEventTrack createTrack(PhotoEventTrackRequestDTO request) {
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

        return trackRepository.save(track);
    }

    /**
     * Returns all participation records for a specific photo.
     *
     * @param photoId the id of the photo whose event history is requested.
     * @return a list of {@link PhotoEventTrack} entities; empty if none exist.
     * @throws EntityNotFoundException if no photo with the given id exists.
     */
    public List<PhotoEventTrack> findByPhoto(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo with id " + photoId + " was not found."));
        return trackRepository.findByPhoto(photo);
    }

    /**
     * Returns all participation records for a specific event.
     *
     * @param eventId the id of the event whose photo entries are requested.
     * @return a list of {@link PhotoEventTrack} entities; empty if none exist.
     * @throws EntityNotFoundException if no event with the given id exists.
     */
    public List<PhotoEventTrack> findByEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Event with id " + eventId + " was not found."));
        return trackRepository.findByEvent(event);
    }
}
