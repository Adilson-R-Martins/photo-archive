package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.EventRequestDTO;
import br.com.cameraeluz.acervo.models.Event;
import br.com.cameraeluz.acervo.repositories.EventRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    /**
     * Lists all registered events.
     * Accessible by any authenticated user.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventRepository.findAll());
    }

    /**
     * Creates a new photography event.
     * Restricted to administrators and editors.
     * * @param eventRequest The validated payload
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<Event> createEvent(@Valid @RequestBody EventRequestDTO eventRequest) {

        Event event = new Event();
        event.setName(eventRequest.getName());
        event.setType(eventRequest.getType());
        event.setComplement(eventRequest.getComplement());
        event.setAffiliation(eventRequest.getAffiliation());
        event.setCategory(eventRequest.getCategory());
        event.setCity(eventRequest.getCity());
        event.setState(eventRequest.getState());
        event.setCountry(eventRequest.getCountry());
        event.setEventDate(eventRequest.getEventDate());

        Event savedEvent = eventRepository.save(event);

        return ResponseEntity.ok(savedEvent);
    }
}