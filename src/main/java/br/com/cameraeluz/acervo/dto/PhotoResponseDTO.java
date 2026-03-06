package br.com.cameraeluz.acervo.dto;

import br.com.cameraeluz.acervo.models.ExifData;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class PhotoResponseDTO {
    private Long id;
    private String title;
    private String artisticAuthorName;
    private Set<String> categories;
    private String viewUrl;
    private String downloadUrl;
    private ExifData metadata;

    // List of event participations and awards
    private List<TrackInfoDTO> eventHistory;

    @Data
    public static class TrackInfoDTO {
        private String eventName;
        private String resultDescription;
        private String honorReceived;
        private String eventDate;
    }
}