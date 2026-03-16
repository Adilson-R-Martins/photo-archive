package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.DownloadPermission;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link DownloadPermission}.
 *
 * <p>The {@code findByUserIdAndPhotoIdWithLock} method acquires a PESSIMISTIC_WRITE
 * (SELECT … FOR UPDATE) lock, preventing concurrent transactions from reading a stale
 * {@code downloadCount} and simultaneously exceeding the {@code downloadLimit}.</p>
 */
@Repository
public interface DownloadPermissionRepository extends JpaRepository<DownloadPermission, UUID> {

    /**
     * Fetches the permission record for the given user/photo pair and acquires a
     * row-level PESSIMISTIC_WRITE lock. Any concurrent transaction that attempts the
     * same query will block until this transaction commits or rolls back, ensuring
     * that {@code downloadCount} increments are atomic.
     *
     * <p>Must be called inside a {@code @Transactional} method.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT dp FROM DownloadPermission dp WHERE dp.user.id = :userId AND dp.photo.id = :photoId")
    Optional<DownloadPermission> findByUserIdAndPhotoIdWithLock(
            @Param("userId") Long userId,
            @Param("photoId") Long photoId
    );

    /**
     * Reads the permission without locking. Safe for use in non-concurrent paths
     * (e.g., the grant flow, which is not contended at the row level).
     */
    @Query("SELECT dp FROM DownloadPermission dp WHERE dp.user.id = :userId AND dp.photo.id = :photoId")
    Optional<DownloadPermission> findByUserIdAndPhotoId(
            @Param("userId") Long userId,
            @Param("photoId") Long photoId
    );

    /** Returns a page of permissions associated with a given photo (admin/editor listing). */
    @Query("SELECT dp FROM DownloadPermission dp WHERE dp.photo.id = :photoId")
    Page<DownloadPermission> findAllByPhotoId(@Param("photoId") Long photoId, Pageable pageable);

    /** Returns a page of permissions granted to a given user (admin/editor listing). */
    @Query("SELECT dp FROM DownloadPermission dp WHERE dp.user.id = :userId")
    Page<DownloadPermission> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Returns {@code true} if the given user holds an <em>active</em> download permission
     * for the given photo.
     *
     * <p>A permission is considered active when both conditions hold:</p>
     * <ul>
     *   <li>{@code is_revoked = false} — the permission has not been explicitly revoked.</li>
     *   <li>{@code download_count < download_limit} — the usage quota has not been exhausted.</li>
     * </ul>
     *
     * <p>This method is used by the visibility system to grant read access (view + listing)
     * to {@link br.com.cameraeluz.acervo.models.enums.Visibility#PRIVATE} photos for users
     * who hold an active permission — without acquiring the PESSIMISTIC_WRITE lock that
     * is required only during the download counter increment path.</p>
     *
     * @param userId  the id of the user whose permission is checked.
     * @param photoId the id of the photo being accessed.
     * @return {@code true} if an active permission exists; {@code false} otherwise.
     */
    @Query("SELECT COUNT(dp) > 0 FROM DownloadPermission dp " +
           "WHERE dp.user.id = :userId " +
           "AND dp.photo.id = :photoId " +
           "AND dp.revoked = false " +
           "AND dp.downloadCount < dp.downloadLimit")
    boolean hasActivePermission(
            @Param("userId") Long userId,
            @Param("photoId") Long photoId
    );
}
