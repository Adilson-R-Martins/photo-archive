package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.models.Role;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.payload.request.LoginRequest;
import br.com.cameraeluz.acervo.payload.request.SignupRequest;
import br.com.cameraeluz.acervo.payload.response.JwtResponse;
import br.com.cameraeluz.acervo.repositories.RoleRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import br.com.cameraeluz.acervo.security.JwtUtils;
import br.com.cameraeluz.acervo.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for user authentication and account registration.
 *
 * <p>Authentication delegates credential validation to Spring Security's
 * {@link AuthenticationManager} and returns a signed JWT on success.
 * Registration enforces uniqueness constraints on both username and e-mail
 * before persisting the new account with the least-privileged role.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    /**
     * Authenticates a user and returns a JWT response.
     *
     * @param loginRequest the login credentials (username and password).
     * @return a {@link JwtResponse} containing the signed token, user id,
     *         username, e-mail, and the list of granted roles.
     * @throws org.springframework.security.core.AuthenticationException if the
     *         credentials are invalid or the account is disabled.
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles);
    }

    /**
     * Registers a new user account with the default {@code ROLE_USER} role.
     *
     * @param signUpRequest the registration data (username, e-mail, password).
     * @throws RuntimeException if the username is already taken or the e-mail
     *         address is already registered.
     */
    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException(
                    "Username '" + signUpRequest.getUsername() + "' is already taken. Choose a different username.");
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException(
                    "Email address '" + signUpRequest.getEmail() + "' is already registered. Use a different address or sign in.");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setRoles(Set.of(getRoleByName("ROLE_USER"))); // New accounts always receive the least-privileged role.

        userRepository.save(user);
    }

    /**
     * Retrieves a {@link Role} by its name from the repository.
     *
     * @param roleName the exact role name (e.g., {@code "ROLE_USER"}).
     * @return the matching {@link Role} entity.
     * @throws RuntimeException if the role is not present in the database.
     */
    private Role getRoleByName(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(
                        "Role '" + roleName + "' is not configured in the system. Contact an administrator."));
    }
}
