package br.com.cameraeluz.acervo.dto;

import br.com.cameraeluz.acervo.models.enums.Visibility;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * DTO for photo data returned by the read endpoints.
 *
 * <p>Includes authorship, technical metadata, category assignments,
 * event/award history, access URLs, and the photo's current
 * {@link Visibility} tier.</p>
 */
@Data
@NoArgsConstructor
public class PhotoResponseDTO {
    private Long id;
    private String title;
    private String artisticAuthorName;

    /**
     * Current access policy for this photo.
     *
     * @see Visibility
     */
    private Visibility visibility;

    private Set<String> categories;
    private String viewUrl;
    private String downloadUrl;
    private ExifDataDTO metadata;

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