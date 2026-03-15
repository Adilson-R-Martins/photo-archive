package br.com.cameraeluz.acervo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks download permissions granted to a specific user for a specific photo.
 *
 * <p>Enforces per-user download limits and supports explicit revocation.
 * The unique constraint on (user_id, photo_id) ensures at most one active
 * permission record per user-photo pair (upsert semantics on grant).</p>
 *
 * <p>Atomic increment of {@code downloadCount} is guaranteed via
 * PESSIMISTIC_WRITE locking at the repository layer.</p>
 */
@Entity
@Table(
    name = "download_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_photo_permission",
        columnNames = {"user_id", "photo_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class DownloadPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** The user who was granted this download permission. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The photo this permission applies to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    /** Cumulative number of times the user has downloaded this photo. */
    @Column(name = "download_count", nullable = false)
    private int downloadCount = 0;

    /**
     * Maximum number of downloads allowed for this permission.
     * Default is 1 (single-use download link).
     */
    @Column(name = "download_limit", nullable = false)
    private int downloadLimit = 1;

    /**
     * When {@code true}, this permission is explicitly denied regardless of counts.
     * A revoked permission is never re-activated automatically — it must be re-granted.
     */
    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    /** Timestamp when this permission was first created (immutable). */
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    /** ID of the user who granted this permission (audit trail). */
    @Column(name = "granted_by_user_id")
    private Long grantedByUserId;

    @PrePersist
    protected void onCreate() {
        this.grantedAt = LocalDateTime.now();
    }
}
