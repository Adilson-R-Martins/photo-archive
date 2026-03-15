package br.com.cameraeluz.acervo.dto;

import lombok.Data;

import java.util.List;

/**
 * Response DTO for the authenticated user's own profile.
 *
 * <p>Returned by {@code GET /api/auth/me}. All fields are sourced from the
 * JWT-backed {@code UserDetailsImpl} already held in the security context,
 * so no extra database round-trip is required.</p>
 */
@Data
public class UserProfileDTO {

    private Long id;
    private String username;
    private String email;
    private boolean active;

    /**
     * Full Spring Security authority strings (e.g., {@code "ROLE_EDITOR"}).
     */
    private List<String> roles;
}
