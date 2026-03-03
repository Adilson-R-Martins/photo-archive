package br.com.cameraeluz.acervo.specs;

import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class PhotoSpecifications {

    public static Specification<Photo> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null :
                cb.isMember(categoryId, root.get("categories"));
    }

    public static Specification<Photo> hasAuthor(Long userId) {
        return (root, query, cb) -> userId == null ? null :
                cb.equal(root.get("uploadedBy").get("id"), userId);
    }

    public static Specification<Photo> fromEventYear(Integer year) {
        return (root, query, cb) -> {
            if (year == null) return null;
            // Join com PhotoEventTrack -> Event
            Join<Photo, PhotoEventTrack> eventTrackJoin = root.join("eventTracks");
            return cb.equal(cb.function("YEAR", Integer.class, eventTrackJoin.get("event").get("eventDate")), year);
        };
    }
}