package br.com.cameraeluz.acervo.repositories.specs;

import br.com.cameraeluz.acervo.models.Category;
import br.com.cameraeluz.acervo.models.Event;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class PhotoSpecifications {

    /**
     * Filters photos by category ID using a Join.
     */
    public static Specification<Photo> hasCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            // Fazemos um join com a tabela de categorias e filtramos pelo ID
            Join<Photo, Category> categoriesJoin = root.join("categories");
            return cb.equal(categoriesJoin.get("id"), categoryId);
        };
    }

    /**
     * Filters photos by author (User) ID.
     */
    public static Specification<Photo> hasAuthor(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) return null;
            // Join com a entidade User (uploadedBy) e filtramos pelo ID
            return cb.equal(root.get("uploadedBy").get("id"), userId);
        };
    }

    /**
     * Filters photos by the year of the associated event.
     */
    public static Specification<Photo> fromEventYear(Integer year) {
        return (root, query, cb) -> {
            if (year == null) return null;

            // 1. Join Photo -> PhotoEventTrack (nome do campo na classe Photo)
            Join<Photo, PhotoEventTrack> eventTrackJoin = root.join("eventTracks");

            // 2. Join PhotoEventTrack -> Event (nome do campo na classe PhotoEventTrack)
            Join<PhotoEventTrack, Event> eventJoin = eventTrackJoin.join("event");

            // 3. Extrai o ano da data do evento usando a função SQL YEAR
            return cb.equal(cb.function("YEAR", Integer.class, eventJoin.get("eventDate")), year);
        };
    }
}