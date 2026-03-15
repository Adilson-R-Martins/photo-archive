package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Photo} entity.
 */
@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long>, JpaSpecificationExecutor<Photo> {

    /** Returns all active photos. */
    List<Photo> findAllByActiveTrue();

    /** Returns all active photos uploaded by the given user. */
    List<Photo> findByUploadedByAndActiveTrue(User user);

    /** Returns a paginated list of active photos. */
    Page<Photo> findAllByActiveTrue(Pageable pageable);

    Optional<Photo> findByWebOptimizedPath(String webOptimizedPath);

}