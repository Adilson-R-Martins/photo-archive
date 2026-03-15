package br.com.cameraeluz.acervo.dto;

import br.com.cameraeluz.acervo.models.enums.EventType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only projection of a {@link br.com.cameraeluz.acervo.models.PhotoEventTrack} record.
 *
 * <p>Exposes the participation details without leaking JPA entity references,
 * preventing JSON serialization recursion between {@code PhotoEventTrack → Photo →
 * eventTracks → PhotoEventTrack}.</p>
 */
@Data
public class PhotoEventTrackResponseDTO {

    /** Internal id of the participation record. */
    private Long id;

    /** Id of the photo that participated. */
    private Long photoId;

    /** Display title of the photo. */
    private String photoTitle;

    /** Id of the event. */
    private Long eventId;

    /** Name of the event. */
    private String eventName;

    /** Type of the event (CONTEST or EXHIBITION). */
    private EventType eventType;

    /** Official date of the event. */
    private LocalDate eventDate;

    /** Id of the result type achieved. */
    private Long resultTypeId;

    /** Human-readable description of the result (e.g., "1st Place", "Acceptance"). */
    private String resultDescription;

    /** Specific honor received (e.g., "Gold Medal", "Ribbon"). Nullable. */
    private String honorReceived;

    /** Optional jury comments or notes about this participation. */
    private String notes;

    /** Timestamp when this record was registered in the system. */
    private LocalDateTime registeredAt;
}
