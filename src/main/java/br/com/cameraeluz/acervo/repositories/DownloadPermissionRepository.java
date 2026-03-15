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
}
