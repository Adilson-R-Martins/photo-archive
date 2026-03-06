package br.com.cameraeluz.acervo.dto;

import br.com.cameraeluz.acervo.models.ExifData;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Set;

/**
 * DTO for photo data.
 * Includes traceable event history and sanitized metadata.
 */
@Data
@NoArgsConstructor
public class PhotoResponseDTO {
    private Long id;
    private String title;
    private String artisticAuthorName;
    private Set<String> categories;
    private String viewUrl;
    private String downloadUrl;
    private ExifData metadata;

    /**
     * List of all events and awards associated with this specific photo.
     */
    private List<TrackInfoDTO> eventHistory;

    @Data
    @NoArgsConstructor
    public static class TrackInfoDTO {
        private String eventName;
        private String resultDescription;
        private String honorReceived;
        private String eventDate;
    }
}