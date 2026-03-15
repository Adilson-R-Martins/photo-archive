package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.DownloadPermissionRequestDTO;
import br.com.cameraeluz.acervo.dto.DownloadPermissionResponseDTO;
import br.com.cameraeluz.acervo.security.UserDetailsImpl;
import br.com.cameraeluz.acervo.services.DownloadPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

        UserDetailsImpl caller = (UserDetailsImpl) authentication.getPrincipal();
        DownloadPermissionResponseDTO response =
                permissionService.grantPermission(caller.getId(), extractRoles(authentication), request);
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

        UserDetailsImpl caller = (UserDetailsImpl) authentication.getPrincipal();
        permissionService.revokePermission(caller.getId(), extractRoles(authentication), permissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists download permissions with optional filters.
     * Restricted to ADMIN and EDITOR roles.
     *
     * @param photoId Filter by photo (optional).
     * @param userId  Filter by user (optional). Ignored when {@code photoId} is present.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<DownloadPermissionResponseDTO>> listPermissions(
            @RequestParam(required = false) Long photoId,
            @RequestParam(required = false) Long userId) {

        return ResponseEntity.ok(permissionService.listPermissions(photoId, userId));
    }

    // -------------------------------------------------------------------------

    private Collection<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
    }
}
