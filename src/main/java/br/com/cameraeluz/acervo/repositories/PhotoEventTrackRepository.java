package br.com.cameraeluz.acervo.repositories;

import br.com.cameraeluz.acervo.models.Event;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoEventTrackRepository extends JpaRepository<PhotoEventTrack, Long> {
    List<PhotoEventTrack> findByPhoto(Photo photo);

    List<PhotoEventTrack> findByEvent(Event event);
}