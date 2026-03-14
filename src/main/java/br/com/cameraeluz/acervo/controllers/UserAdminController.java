package br.com.cameraeluz.acervo.controllers;

import br.com.cameraeluz.acervo.models.Role;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.payload.response.MessageResponse;
import br.com.cameraeluz.acervo.repositories.RoleRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Updates the roles of a specific user.
     * Restricted exclusively to administrators.
     *
     * @param id    The ID of the user to be updated
     * @param roles Set of role names (ex: "ROLE_EDITOR", "ROLE_AUTHOR")
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Set<String> roles) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + id));

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : roles) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + roleName));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Roles do usuário atualizadas com sucesso."));
    }

    /**
     * Activates or deactivates a user account.
     * Restricted exclusively to administrators.
     *
     * @param id     The ID of the user
     * @param active true to activate, false to deactivate
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + id));

        user.setActive(active);
        userRepository.save(user);

        String status = active ? "ativado" : "desativado";
        return ResponseEntity.ok(new MessageResponse("Usuário " + status + " com sucesso."));
    }
}