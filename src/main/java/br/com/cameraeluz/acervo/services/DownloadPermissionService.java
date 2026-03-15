package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.DownloadPermissionRequestDTO;
import br.com.cameraeluz.acervo.dto.DownloadPermissionResponseDTO;
import br.com.cameraeluz.acervo.exceptions.DownloadLimitReachedException;
import br.com.cameraeluz.acervo.exceptions.DownloadRevokedException;
import br.com.cameraeluz.acervo.models.DownloadPermission;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.repositories.DownloadPermissionRepository;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

/**
 * Core service for the download permission system.
 *
 * <h3>Design: Guard Pattern with Strict Precedence</h3>
 * <ol>
 *   <li><b>ADMIN / EDITOR</b> — absolute access, always allowed.</li>
 *   <li><b>Photo owner</b> — unlimited access, no counter incremented.</li>
 *   <li><b>Permission record</b> — validated and consumed atomically via
 *       PESSIMISTIC_WRITE lock (SELECT FOR UPDATE) to prevent race conditions.</li>
 * </ol>
 *
 * <h3>Revocation rules</h3>
 * Only ADMIN, EDITOR, or the photo's owner may revoke a permission.
 * An AUTHOR attempting to revoke a permission for a photo they do not own
 * receives {@code 403 Forbidden}.
 */
@Service
@RequiredArgsConstructor
public class DownloadPermissionService {

    private static final String ROLE_ADMIN  = "ROLE_ADMIN";
    private static final String ROLE_EDITOR = "ROLE_EDITOR";

    private final DownloadPermissionRepository permissionRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    // =========================================================================
    // Guard: canDownload
    // =========================================================================

    /**
     * Enforces download authorization with strict precedence rules and, for the
     * permission-record path, atomically increments {@code downloadCount} inside
     * the PESSIMISTIC_WRITE-locked transaction (SELECT FOR UPDATE).
     *
     * <p>Callers must supply the Spring Security authority strings (e.g.,
     * {@code "ROLE_ADMIN"}) so that no extra DB round-trip is needed for role checks.</p>
     *
     * @param userId    ID of the requesting user.
     * @param photoId   ID of the target photo.
     * @param userRoles Spring Security authority strings for the requesting user.
     * @throws DownloadRevokedException      if the permission record is revoked.
     * @throws DownloadLimitReachedException if the download limit is exhausted.
     * @throws EntityNotFoundException       if the photo or permission record is not found.
     */
    @Transactional
    public void canDownload(Long userId, Long photoId, Collection<String> userRoles) {

        // Precedence 1: ADMIN / EDITOR — absolute access, no limit tracking.
        if (hasPrivilegedRole(userRoles)) {
            return;
        }

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo not found with id: " + photoId));

        // Precedence 2: Photo owner — unlimited access, no counter incremented.
        if (photo.getUploadedBy().getId().equals(userId)) {
            return;
        }

        // Precedence 3: Permission record — atomic validate + consume.
        // The PESSIMISTIC_WRITE lock serialises concurrent transactions on this row,
        // preventing two threads from both reading downloadCount = N and both
        // deciding they are within the limit before either has incremented it.
        DownloadPermission permission = permissionRepository
                .findByUserIdAndPhotoIdWithLock(userId, photoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No download permission found for this user and photo. "
                        + "Contact the archive administrator."));

        if (permission.isRevoked()) {
            throw new DownloadRevokedException(
                    "Your download access for this photo has been revoked.");
        }

        if (permission.getDownloadCount() >= permission.getDownloadLimit()) {
            throw new DownloadLimitReachedException(
                    String.format("Download limit of %d reached for this photo.",
                            permission.getDownloadLimit()));
        }

        // Atomic increment — safe because we hold the row-level write lock.
        permission.setDownloadCount(permission.getDownloadCount() + 1);
        permissionRepository.save(permission);
    }

    // =========================================================================
    // Grant permission
    // =========================================================================

    /**
     * Grants (or updates) a download permission for a specific user/photo pair.
     *
     * <p>Upsert semantics: if a permission already exists for the same user/photo
     * pair, its limit is updated and any revocation is cleared. A new record is
     * created otherwise.</p>
     *
     * <p>Authorization: only ADMIN, EDITOR, or the photo's uploader may grant.</p>
     *
     * @param granterId    ID of the user performing the grant.
     * @param granterRoles Spring Security authority strings of the granter.
     * @param request      Grant details (target userId, photoId, downloadLimit).
     * @return The persisted permission as a response DTO.
     * @throws AccessDeniedException   if the granter is not authorized.
     * @throws EntityNotFoundException if the photo or target user is not found.
     */
    @Transactional
    public DownloadPermissionResponseDTO grantPermission(
            Long granterId,
            Collection<String> granterRoles,
            DownloadPermissionRequestDTO request) {

        Photo photo = photoRepository.findById(request.getPhotoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo not found with id: " + request.getPhotoId()));

        boolean isPhotoOwner = photo.getUploadedBy().getId().equals(granterId);
        if (!hasPrivilegedRole(granterRoles) && !isPhotoOwner) {
            throw new AccessDeniedException(
                    "Only ADMIN, EDITOR, or the photo owner can grant download permissions.");
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Target user not found with id: " + request.getUserId()));

        // Upsert: update existing or create new.
        DownloadPermission permission = permissionRepository
                .findByUserIdAndPhotoId(request.getUserId(), request.getPhotoId())
                .orElseGet(() -> {
                    DownloadPermission p = new DownloadPermission();
                    p.setUser(targetUser);
                    p.setPhoto(photo);
                    p.setGrantedByUserId(granterId);
                    return p;
                });

        permission.setDownloadLimit(request.getDownloadLimit());
        permission.setRevoked(false); // Reactivate if previously revoked.

        return toResponseDTO(permissionRepository.save(permission));
    }

    // =========================================================================
    // Revoke permission
    // =========================================================================

    /**
     * Revokes an existing download permission (sets {@code is_revoked = true}).
     *
     * <p>Authorization rules (strict):</p>
     * <ul>
     *   <li>ADMIN or EDITOR: may revoke any permission.</li>
     *   <li>AUTHOR (or any other role): may revoke <em>only</em> permissions linked
     *       to a photo they own. Attempting to revoke another owner's photo permission
     *       results in {@code 403 Forbidden}.</li>
     * </ul>
     *
     * @param requesterId    ID of the user requesting revocation.
     * @param requesterRoles Spring Security authority strings of the requester.
     * @param permissionId   UUID of the permission to revoke.
     * @throws AccessDeniedException   if the requester is not authorized.
     * @throws EntityNotFoundException if the permission is not found.
     */
    @Transactional
    public void revokePermission(
            Long requesterId,
            Collection<String> requesterRoles,
            UUID permissionId) {

        DownloadPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Permission not found with id: " + permissionId));

        boolean isPhotoOwner = permission.getPhoto().getUploadedBy().getId().equals(requesterId);
        if (!hasPrivilegedRole(requesterRoles) && !isPhotoOwner) {
            throw new AccessDeniedException(
                    "Only ADMIN, EDITOR, or the photo owner can revoke download permissions.");
        }

        permission.setRevoked(true);
        permissionRepository.save(permission);
    }

    // =========================================================================
    // Listing (admin / editor)
    // =========================================================================

    /**
     * Returns a paginated list of permission records, optionally filtered by
     * {@code photoId} or {@code userId}. When both are {@code null}, all records
     * are returned. {@code photoId} takes precedence over {@code userId} when both
     * are provided.
     *
     * @param photoId  optional filter by photo id.
     * @param userId   optional filter by user id.
     * @param pageable pagination and sorting parameters.
     * @return a {@link Page} of {@link DownloadPermissionResponseDTO}.
     */
    @Transactional(readOnly = true)
    public Page<DownloadPermissionResponseDTO> listPermissions(Long photoId, Long userId, Pageable pageable) {
        Page<DownloadPermission> permissions;

        if (photoId != null) {
            permissions = permissionRepository.findAllByPhotoId(photoId, pageable);
        } else if (userId != null) {
            permissions = permissionRepository.findAllByUserId(userId, pageable);
        } else {
            permissions = permissionRepository.findAll(pageable);
        }

        return permissions.map(this::toResponseDTO);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private boolean hasPrivilegedRole(Collection<String> roles) {
        return roles.contains(ROLE_ADMIN) || roles.contains(ROLE_EDITOR);
    }

    private DownloadPermissionResponseDTO toResponseDTO(DownloadPermission p) {
        return DownloadPermissionResponseDTO.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .username(p.getUser().getUsername())
                .photoId(p.getPhoto().getId())
                .photoTitle(p.getPhoto().getTitle())
                .downloadCount(p.getDownloadCount())
                .downloadLimit(p.getDownloadLimit())
                .revoked(p.isRevoked())
                .grantedAt(p.getGrantedAt())
                .grantedByUserId(p.getGrantedByUserId())
                .build();
    }
}
