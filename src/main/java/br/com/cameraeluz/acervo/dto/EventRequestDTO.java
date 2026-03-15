package br.com.cameraeluz.acervo.dto;

import br.com.cameraeluz.acervo.models.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for creating or updating an Event.
 * Enforces strict validation rules to maintain data integrity.
 */
@Data
public class EventRequestDTO {

    @NotBlank(message = "Event name is required.")
    private String name;

    @NotNull(message = "Event type is required. Accepted values: CONTEST, EXHIBITION.")
    private EventType type;

    private String complement;
    private String affiliation;

    /** Category name within the event scope (e.g., Monochrome, Color). */
    private String category;

    private String city;
    private String state;
    private String country;

    @NotNull(message = "Event date is required for traceability purposes.")
    private LocalDate eventDate;
}
