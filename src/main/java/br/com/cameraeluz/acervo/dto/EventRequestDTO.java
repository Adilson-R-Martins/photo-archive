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

    @NotBlank(message = "O nome do evento é obrigatório.")
    private String name;

    @NotNull(message = "O tipo de evento é obrigatório (CONTEST ou EXHIBITION).")
    private EventType type;

    private String complement;
    private String affiliation;
    private String category; // Categoria dentro do evento (Ex: Monocromo, Cor)

    private String city;
    private String state;
    private String country;

    @NotNull(message = "A data do evento é obrigatória para fins de rastreabilidade.")
    private LocalDate eventDate;
}