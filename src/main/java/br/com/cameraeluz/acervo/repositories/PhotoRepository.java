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

    // Busca apenas fotos ativas para a galeria geral
    List<Photo> findAllByActiveTrue();

    // Busca fotos ativas de um autor específico (útil para o perfil do fotógrafo)
    List<Photo> findByUploadedByAndActiveTrue(User user);

    // Versão paginada para performance
    Page<Photo> findAllByActiveTrue(Pageable pageable);

    Optional<Photo> findByWebOptimizedPath(String webOptimizedPath);

}