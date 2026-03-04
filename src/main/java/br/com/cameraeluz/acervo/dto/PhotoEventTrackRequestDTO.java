package br.com.cameraeluz.acervo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PhotoEventTrackRequestDTO {
    @NotNull(message = "ID da foto é obrigatório.")
    private Long photoId;

    @NotNull(message = "ID do evento é obrigatório.")
    private Long eventId;

    @NotNull(message = "ID do tipo de resultado é obrigatório.")
    private Long resultTypeId;

    private String honor;
    private String notes;
}