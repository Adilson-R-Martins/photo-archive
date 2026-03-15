package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.dto.UserProfileDTO;
import br.com.cameraeluz.acervo.payload.request.LoginRequest;
import br.com.cameraeluz.acervo.payload.request.SignupRequest;
import br.com.cameraeluz.acervo.payload.response.JwtResponse;
import br.com.cameraeluz.acervo.payload.response.MessageResponse;
import br.com.cameraeluz.acervo.security.SecurityUtils;
import br.com.cameraeluz.acervo.security.UserDetailsImpl;
import br.com.cameraeluz.acervo.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticateUser(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        return ResponseEntity.ok(new MessageResponse("User registered successfully."));
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>All data is sourced from the JWT-backed security context — no database
     * round-trip is needed. The response includes the user's id, username, email,
     * active status, and the list of granted roles.</p>
     *
     * @param authentication the current security context (injected by Spring).
     * @return the caller's profile as a {@link UserProfileDTO}.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = SecurityUtils.getUserDetails(authentication);

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(userDetails.getId());
        dto.setUsername(userDetails.getUsername());
        dto.setEmail(userDetails.getEmail());
        dto.setActive(userDetails.isEnabled());
        dto.setRoles(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(dto);
    }
}