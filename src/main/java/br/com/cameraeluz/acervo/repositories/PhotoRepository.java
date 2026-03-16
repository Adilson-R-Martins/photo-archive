package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * Finds a photo by its web-optimised path, eagerly fetching {@code uploadedBy}.
     *
     * <p>The {@code uploadedBy} association is fetched eagerly here because the
     * result is used by {@link br.com.cameraeluz.acervo.services.PhotoService#isVisibleTo}
     * to evaluate {@link br.com.cameraeluz.acervo.models.enums.Visibility#PRIVATE} access
     * outside of the loading transaction. Without the eager fetch the detached entity
     * would throw {@code LazyInitializationException} at the visibility check.</p>
     */
    @EntityGraph(attributePaths = {"uploadedBy"})
    Optional<Photo> findByWebOptimizedPath(String webOptimizedPath);

    /**
     * Finds a photo by its primary key, eagerly fetching {@code uploadedBy}.
     *
     * <p>Overrides {@link JpaRepository#findById} to include {@code uploadedBy}
     * in the initial query so that the visibility check in
     * {@link br.com.cameraeluz.acervo.services.PhotoService#isVisibleTo} can access
     * {@code photo.getUploadedBy().getId()} on the detached entity without
     * triggering a {@code LazyInitializationException}.</p>
     */
    @Override
    @EntityGraph(attributePaths = {"uploadedBy"})
    Optional<Photo> findById(Long id);

}