package br.com.cameraeluz.acervo.dto;

import lombok.Data;

import java.util.Set;

/**
 * Summary DTO used in admin user-management responses.
 *
 * <p>Returned by {@code GET /api/admin/users} (paginated list) and
 * {@code GET /api/admin/users/{id}} (single user detail).
 * Exposes only the fields an administrator needs to manage accounts —
 * the password hash is never included.</p>
 */
@Data
public class UserSummaryDTO {

    private Long id;
    private String username;
    private String email;
    private String artisticName;
    private boolean active;

    /**
     * Full Spring Security authority strings assigned to this user
     * (e.g., {@code "ROLE_EDITOR"}).
     */
    private Set<String> roles;
}
