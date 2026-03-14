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

@Service
@RequiredArgsConstructor // <-- Lombok gera o construtor com os campos 'final'
public class PhotoEventTrackService {

    // O uso do 'final' é obrigatório para o @RequiredArgsConstructor funcionar
    private final PhotoEventTrackRepository trackRepository;
    private final PhotoRepository photoRepository;
    private final EventRepository eventRepository;
    private final ResultTypeRepository resultTypeRepository;

    public PhotoEventTrack createTrack(PhotoEventTrackRequestDTO request) {
        Photo photo = photoRepository.findById(request.getPhotoId())
                .orElseThrow(() -> new EntityNotFoundException("Foto não encontrada com o ID: " + request.getPhotoId()));

        if (!photo.isActive()) {
            throw new IllegalStateException("Esta foto está inativa e não pode participar de eventos.");
        }

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado com o ID: " + request.getEventId()));

        ResultType resultType = resultTypeRepository.findById(request.getResultTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de Resultado não encontrado com o ID: " + request.getResultTypeId()));

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
     */
    public List<PhotoEventTrack> findByPhoto(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Foto não encontrada com o ID: " + photoId));
        return trackRepository.findByPhoto(photo);
    }

    /**
     * Returns all participation records for a specific event.
     */
    public List<PhotoEventTrack> findByEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Evento não encontrado com o ID: " + eventId));
        return trackRepository.findByEvent(event);
    }
}