package br.com.cameraeluz.acervo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Set;

/**
 * Request body for the admin endpoint that replaces a user's full role set.
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>The set must not be empty — sending an empty set would silently strip all
 *       roles from the target user, which is almost certainly unintended.</li>
 *   <li>Each role name must match one of the recognised system role strings.
 *       Unknown role names are rejected before reaching the service layer.</li>
 * </ul>
 */
@Data
public class UserRolesUpdateRequest {

    @NotEmpty(message = "At least one role must be provided. Sending an empty set would remove all roles.")
    private Set<
            @Pattern(
                    regexp = "ROLE_(ADMIN|EDITOR|AUTHOR|GUEST|USER)",
                    message = "Invalid role name. Allowed values: ROLE_ADMIN, ROLE_EDITOR, ROLE_AUTHOR, ROLE_GUEST, ROLE_USER."
            ) String> roles;
}
