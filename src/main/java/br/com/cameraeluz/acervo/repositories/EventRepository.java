package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Event entity operations.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
}