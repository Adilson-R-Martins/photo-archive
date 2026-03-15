package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

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
     *
     * <p>@BatchSize instructs Hibernate to load up to 10 Event rows per SQL round-trip
     * when this association is accessed across a collection of PhotoEventTrack objects,
     * reducing the N+1 effect when building search result DTOs.</p>
     */
    @BatchSize(size = 10)
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * The official result achieved (e.g., 1st Place, Acceptance).
     *
     * <p>@BatchSize mirrors the same batching strategy applied to {@link #event}.</p>
     */
    @BatchSize(size = 10)
    @ManyToOne(optional = false)
    @JoinColumn(name = "result_type_id", nullable = false)
    private ResultType resultType;

    /**
     * Specific honors received (e.g., Gold Medal, Ribbon).
     */
    @Column(name = "honor_received")
    private String honorReceived;

    /**
     * Optional jury comments or notes about this participation.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Timestamp of when this participation record was registered.
     * Immutable after creation — essential for audit traceability.
     */
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }
}