package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.UserSummaryDTO;
import br.com.cameraeluz.acervo.models.Role;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.repositories.RoleRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for admin-level user account management operations.
 *
 * <p>Provides transactional operations to replace a user's role set and to
 * toggle the active status of an account. Both write operations are wrapped in
 * a single transaction so that partial failures (e.g., a role lookup succeeding
 * but the final save failing) are automatically rolled back.</p>
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Returns a paginated list of all user accounts.
     *
     * @param pageable pagination and sorting parameters.
     * @return a {@link Page} of {@link UserSummaryDTO}.
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryDTO> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toSummaryDTO);
    }

    /**
     * Returns the detail of a single user account.
     *
     * @param id the user id.
     * @return the user as a {@link UserSummaryDTO}.
     * @throws EntityNotFoundException if no user with the given id exists.
     */
    @Transactional(readOnly = true)
    public UserSummaryDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with id " + id + " was not found."));
        return toSummaryDTO(user);
    }

    /**
     * Replaces the full set of roles assigned to a user.
     *
     * <p>Any roles previously assigned are discarded and replaced with the
     * exact set provided. All role lookups and the final save are executed
     * within a single transaction.</p>
     *
     * @param userId    the id of the user whose roles will be updated.
     * @param roleNames the set of role name strings to assign
     *                  (e.g., {@code "ROLE_EDITOR"}, {@code "ROLE_AUTHOR"}).
     * @throws EntityNotFoundException if no user or role with the given id/name exists.
     */
    // allEntries = true clears the entire userDetails cache on any role change.
    // A username-keyed eviction would be more surgical but would require an extra
    // username lookup by userId; clearing all is simpler and correct for low-frequency ops.
    @Transactional
    @CacheEvict(value = "userDetails", allEntries = true)
    public void updateUserRoles(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " was not found."));

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Role '" + roleName + "' was not found. "
                            + "Valid roles: ROLE_ADMIN, ROLE_EDITOR, ROLE_AUTHOR, ROLE_GUEST, ROLE_USER."));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        userRepository.save(user);
    }

    private UserSummaryDTO toSummaryDTO(User user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setArtisticName(user.getArtisticName());
        dto.setActive(user.isActive());
        dto.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return dto;
    }

    /**
     * Activates or deactivates a user account.
     *
     * @param userId the id of the user whose status will be changed.
     * @param active {@code true} to activate the account; {@code false} to deactivate it.
     * @throws EntityNotFoundException if no user with the given id exists.
     */
    @Transactional
    @CacheEvict(value = "userDetails", allEntries = true)
    public void updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " was not found."));

        user.setActive(active);
        userRepository.save(user);
    }
}
