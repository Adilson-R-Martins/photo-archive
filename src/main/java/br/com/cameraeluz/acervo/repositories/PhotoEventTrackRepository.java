package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoEventTrackRepository extends JpaRepository<PhotoEventTrack, Long> {
}