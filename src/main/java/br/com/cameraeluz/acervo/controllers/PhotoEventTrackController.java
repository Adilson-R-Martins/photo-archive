package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.PhotoEventTrackRequestDTO;
import br.com.cameraeluz.acervo.models.Event;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import br.com.cameraeluz.acervo.models.ResultType;
import br.com.cameraeluz.acervo.repositories.EventRepository;
import br.com.cameraeluz.acervo.repositories.PhotoEventTrackRepository;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.repositories.ResultTypeRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/tracks")
public class PhotoEventTrackController {

    @Autowired
    private PhotoEventTrackRepository trackRepository; // Resolve 'trackRepository'

    @Autowired
    private PhotoRepository photoRepository; // Resolve 'photoRepository'

    @Autowired
    private EventRepository eventRepository; // Resolve 'eventRepository'

    @Autowired
    private ResultTypeRepository resultTypeRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<PhotoEventTrack> createTrack(@Valid @RequestBody PhotoEventTrackRequestDTO request) {

        Photo photo = photoRepository.findById(request.getPhotoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado"));

        ResultType resultType = resultTypeRepository.findById(request.getResultTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de Resultado não encontrado"));

        PhotoEventTrack track = new PhotoEventTrack();
        track.setPhoto(photo);
        track.setEvent(event);
        track.setResultType(resultType); // Resolve 'setResultType' (veja passo 2)
        track.setHonorReceived(request.getHonor());
        track.setNotes(request.getNotes());

        return ResponseEntity.ok(trackRepository.save(track));
    }
}