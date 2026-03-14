package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    /**
     * Finds a role by its name (e.g., ROLE_ADMIN).
     */
    Optional<Role> findByName(String name);
}