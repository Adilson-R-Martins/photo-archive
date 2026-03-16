package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.EventRequestDTO;
import br.com.cameraeluz.acervo.models.Event;
import br.com.cameraeluz.acervo.services.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing competition events.
 *
 * <p>Uses the declared role hierarchy (ADMIN &gt; EDITOR &gt; AUTHOR &gt; GUEST),
 * so {@code hasRole('GUEST')} matches all authenticated roles and
 * {@code hasRole('EDITOR')} matches EDITOR and ADMIN.</p>
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /** Returns all events. Accessible to any authenticated user. */
    @GetMapping
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.findAll());
    }

    /** Creates a new event. Restricted to EDITOR and ADMIN. */
    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<Event> createEvent(@Valid @RequestBody EventRequestDTO eventRequest) {
        return ResponseEntity.ok(eventService.create(eventRequest));
    }
}