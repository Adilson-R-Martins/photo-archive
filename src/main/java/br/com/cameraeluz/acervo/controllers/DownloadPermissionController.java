package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.DownloadPermissionRequestDTO;
import br.com.cameraeluz.acervo.dto.DownloadPermissionResponseDTO;
import br.com.cameraeluz.acervo.security.SecurityUtils;
import br.com.cameraeluz.acervo.services.DownloadPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for managing download permissions.
 *
 * <ul>
 *   <li>{@code POST   /api/downloads/permissions}         — Grant permission (owner or ADMIN/EDITOR)</li>
 *   <li>{@code DELETE /api/downloads/permissions/{id}}    — Revoke permission (owner or ADMIN/EDITOR)</li>
 *   <li>{@code GET    /api/downloads/permissions}         — List permissions (ADMIN/EDITOR only)</li>
 * </ul>
 *
 * Fine-grained ownership checks (e.g., whether the caller owns the linked photo)
 * are enforced inside {@link DownloadPermissionService} to keep the controller thin.
 */
@RestController
@RequestMapping("/api/downloads/permissions")
@RequiredArgsConstructor
public class DownloadPermissionController {

    private final DownloadPermissionService permissionService;

    /**
     * Grants (or updates) a download permission for a specific user/photo pair.
     *
     * <p>GUEST users are excluded at this level because they cannot own photos
     * and therefore can never satisfy the ownership check in the service layer.
     * Fine-grained authorization (ADMIN/EDITOR vs. photo owner) is still enforced
     * inside {@link DownloadPermissionService#grantPermission}.</p>
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR')")
    public ResponseEntity<DownloadPermissionResponseDTO> grantPermission(
            @Valid @RequestBody DownloadPermissionRequestDTO request,
            Authentication authentication) {

        DownloadPermissionResponseDTO response =
                permissionService.grantPermission(
                        SecurityUtils.getUserDetails(authentication).getId(),
                        SecurityUtils.extractRoles(authentication),
                        request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Revokes an existing download permission.
     *
     * <p>GUEST users are excluded at this level for the same reason as
     * {@link #grantPermission}: they cannot own photos, so they can never
     * satisfy the ownership check in the service layer.</p>
     */
    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'AUTHOR')")
    public ResponseEntity<Void> revokePermission(
            @PathVariable UUID permissionId,
            Authentication authentication) {

        permissionService.revokePermission(
                SecurityUtils.getUserDetails(authentication).getId(),
                SecurityUtils.extractRoles(authentication),
                permissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the calling user's own download permissions, paginated.
     *
     * <p>Allows any authenticated user to inspect which photos they have been
     * granted access to download and how many downloads they have remaining.
     * Only the caller's own permissions are ever returned.</p>
     *
     * @param pageable       pagination and sorting parameters.
     * @param authentication the current security context.
     * @return a paginated list of {@link DownloadPermissionResponseDTO} for the caller.
     */
    @GetMapping("/me")
    public ResponseEntity<Page<DownloadPermissionResponseDTO>> getMyPermissions(
            @PageableDefault(size = 20, sort = "grantedAt",
                    direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Long currentUserId = SecurityUtils.getUserDetails(authentication).getId();
        return ResponseEntity.ok(permissionService.listPermissions(null, currentUserId, pageable));
    }

    /**
     * Returns a paginated list of download permissions with optional filters.
     * Restricted to ADMIN and EDITOR roles.
     *
     * <p>Example: {@code GET /api/downloads/permissions?photoId=5&page=0&size=20&sort=grantedAt,desc}</p>
     *
     * @param photoId  filter by photo id (optional).
     * @param userId   filter by user id (optional). Ignored when {@code photoId} is present.
     * @param pageable pagination and sorting parameters.
     * @return a paginated list of {@link DownloadPermissionResponseDTO}.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Page<DownloadPermissionResponseDTO>> listPermissions(
            @RequestParam(required = false) Long photoId,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20, sort = "grantedAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(permissionService.listPermissions(photoId, userId, pageable));
    }

}
