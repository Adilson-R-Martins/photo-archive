package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracks the trajectory of a photo in specific events.
 * Records results, awards, and participations for traceability.
 */
@Entity
@Table(name = "photo_event_tracks")
@Getter
@Setter
@NoArgsConstructor
public class PhotoEventTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The photo being tracked.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "photo_id")
    private Photo photo;

    /**
     * The event where the photo participated.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * The official result achieved (e.g., 1st Place, Acceptance).
     * Managed by the administrator through the ResultType entity.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "result_type_id", nullable = false)
    private ResultType resultType;

    /**
     * Specific honors received (e.g., Gold Medal, Ribbon).
     * Stored as a string as these names vary greatly between organizers.
     */
    @Column(name = "honor_received")
    private String honorReceived;

    /**
     * Optional field to store jury comments or specific notes about this participation.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
}
