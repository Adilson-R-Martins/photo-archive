package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.EventRequestDTO;
import br.com.cameraeluz.acervo.models.Event;
import br.com.cameraeluz.acervo.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Event create(EventRequestDTO dto) {
        Event event = new Event();
        event.setName(dto.getName());
        event.setType(dto.getType());
        event.setComplement(dto.getComplement());
        event.setAffiliation(dto.getAffiliation());
        event.setCategory(dto.getCategory());
        event.setCity(dto.getCity());
        event.setState(dto.getState());
        event.setCountry(dto.getCountry());
        event.setEventDate(dto.getEventDate());
        return eventRepository.save(event);
    }
}