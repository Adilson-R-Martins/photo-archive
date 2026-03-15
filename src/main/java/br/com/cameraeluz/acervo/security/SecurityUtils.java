package br.com.cameraeluz.acervo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Stateless utility methods for extracting common security context values
 * from a Spring Security {@link Authentication} object.
 *
 * <p>Centralises role-string extraction and principal casting so that
 * controllers do not duplicate this boilerplate.</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Returns the set of authority strings (e.g., {@code "ROLE_ADMIN"}) held
     * by the authenticated principal.
     *
     * @param authentication the current authentication context.
     * @return a collection of authority strings; never {@code null}.
     */
    public static Collection<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * Returns the {@link UserDetailsImpl} principal from the authentication context.
     *
     * @param authentication the current authentication context.
     * @return the typed user-details object.
     * @throws ClassCastException if the principal is not a {@link UserDetailsImpl}.
     */
    public static UserDetailsImpl getUserDetails(Authentication authentication) {
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}
