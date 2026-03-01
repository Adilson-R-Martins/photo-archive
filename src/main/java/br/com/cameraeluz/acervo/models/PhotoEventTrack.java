package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks the trajectory of a photo in specific events.
 * Records results, awards, and participations for traceability.
 */
@Entity
@Table(name = "photo_event_tracks")
@Data
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
    @ManyToOne
    @JoinColumn(name = "result_type_id")
    private ResultType result;

    /**
     * Specific honors received (e.g., Gold Medal, Ribbon).
     * Stored as a string as these names vary greatly between organizers.
     */
    @Column(name = "honor_received")
    private String honor;

    /**
     * Optional field to store jury comments or specific notes about this participation.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
}
