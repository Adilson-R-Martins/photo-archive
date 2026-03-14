package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.payload.request.LoginRequest;
import br.com.cameraeluz.acervo.payload.request.SignupRequest;
import br.com.cameraeluz.acervo.payload.response.JwtResponse;
import br.com.cameraeluz.acervo.payload.response.MessageResponse;
import br.com.cameraeluz.acervo.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(new MessageResponse("Usuário registrado com sucesso!"));
    }
}