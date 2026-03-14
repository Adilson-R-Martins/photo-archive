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

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
    public ResponseEntity<Event> createEvent(@Valid @RequestBody EventRequestDTO eventRequest) {
        return ResponseEntity.ok(eventService.create(eventRequest));
    }
}