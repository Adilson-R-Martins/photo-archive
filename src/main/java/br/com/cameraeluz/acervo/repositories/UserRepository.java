package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by their username for authentication purposes.
     */
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    /**
     * Checks if an email is already registered in the system.
     */
    Boolean existsByEmail(String email);
}