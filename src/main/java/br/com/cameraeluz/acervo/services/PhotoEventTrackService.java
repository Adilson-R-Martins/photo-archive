package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
import br.com.cameraeluz.acervo.models.*;
import br.com.cameraeluz.acervo.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor // <-- Lombok gera o construtor com os campos 'final'
public class PhotoEventTrackService {

    // O uso do 'final' é obrigatório para o @RequiredArgsConstructor funcionar
    private final PhotoEventTrackRepository trackRepository;
    private final PhotoRepository photoRepository;
    private final EventRepository eventRepository;
    private final ResultTypeRepository resultTypeRepository;

    public PhotoEventTrack createTrack(PhotoEventTrackRequestDTO request) {

        // Regra de negócio isolada aqui!
        Photo photo = photoRepository.findById(request.getPhotoId())
                .orElseThrow(() -> new EntityNotFoundException("Foto não encontrada com o ID: " + request.getPhotoId()));

        // VALIDAÇÃO DE NEGÓCIO:
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
}