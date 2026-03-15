package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.UserRolesUpdateRequest;
import br.com.cameraeluz.acervo.dto.UserSummaryDTO;
import br.com.cameraeluz.acervo.payload.response.MessageResponse;
import br.com.cameraeluz.acervo.services.UserAdminService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only REST controller for managing user accounts.
 *
 * <p>All endpoints require the {@code ROLE_ADMIN} authority. Provides operations
 * to replace a user's role set and to toggle the active/inactive status of an account.</p>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    /**
     * Returns a paginated list of all user accounts.
     *
     * @param pageable pagination and sorting parameters (default: 20 per page, ordered by id).
     * @return a paginated list of {@link UserSummaryDTO}.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserSummaryDTO>> listUsers(
            @PageableDefault(size = 20, sort = "id",
                    direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userAdminService.listUsers(pageable));
    }

    /**
     * Returns the detail of a single user account.
     *
     * @param id the id of the user to retrieve.
     * @return the user as a {@link UserSummaryDTO}.
     * @throws EntityNotFoundException if no user with the given id exists.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummaryDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.getUserById(id));
    }

    /**
     * Replaces the full set of roles assigned to a user.
     *
     * <p>Any roles previously assigned are discarded and replaced with the
     * exact set provided in the request body. The payload must contain at least
     * one valid role name; an empty set would silently strip all access from the user.</p>
     *
     * @param id      the id of the user whose roles will be updated.
     * @param request the validated request body containing the new role set.
     * @return {@code 200 OK} with a confirmation message.
     * @throws EntityNotFoundException if no user or role with the given id/name exists.
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRolesUpdateRequest request) {

        userAdminService.updateUserRoles(id, request.getRoles());
        return ResponseEntity.ok(new MessageResponse("User roles updated successfully."));
    }

    /**
     * Activates or deactivates a user account.
     *
     * @param id     the id of the user whose status will be changed.
     * @param active {@code true} to activate the account; {@code false} to deactivate it.
     * @return {@code 200 OK} with a message describing the outcome.
     * @throws EntityNotFoundException if no user with the given id exists.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        userAdminService.updateUserStatus(id, active);
        return ResponseEntity.ok(new MessageResponse(
                String.format("User account %s successfully.", active ? "activated" : "deactivated")));
    }
}
