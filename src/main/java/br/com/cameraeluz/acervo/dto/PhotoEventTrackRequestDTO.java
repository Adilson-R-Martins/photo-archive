package br.com.cameraeluz.acervo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a photo participation record ({@link br.com.cameraeluz.acervo.models.PhotoEventTrack}).
 *
 * <p>Links a photo to an event with a result type, and optionally records
 * the honor received and any additional notes.</p>
 */
@Data
public class PhotoEventTrackRequestDTO {

    @NotNull(message = "Photo ID is required.")
    private Long photoId;

    @NotNull(message = "Event ID is required.")
    private Long eventId;

    @NotNull(message = "Result type ID is required.")
    private Long resultTypeId;

    private String honor;
    private String notes;
}
