package br.com.cameraeluz.acervo.repositories.specs;

import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Technical Specification for dynamic photo filtering.
 * Implements industry standards for complex, multi-parameter searching.
 */
public class PhotoSpecifications {

    /**
     * Builds a dynamic query based on provided filters.
     * @param authorId Search by photographer ID.
     * @param eventId Search by specific event participation.
     * @param resultTypeId Search by specific award/result.
     * @param keyword Global search in title and technical metadata.
     * @return Specification for JPA execution.
     */
    public static Specification<Photo> withAdvancedFilters(
            Long authorId,
            Long eventId,
            Long resultTypeId,
            String keyword) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Author Filter
            if (authorId != null) {
                predicates.add(cb.equal(root.get("uploadedBy").get("id"), authorId));
            }

            // 2. Traceability Filters (Join with Event Tracks)
            if (eventId != null || resultTypeId != null) {
                Join<Photo, PhotoEventTrack> trackJoin = root.join("eventTracks", JoinType.INNER);

                if (eventId != null) {
                    predicates.add(cb.equal(trackJoin.get("event").get("id"), eventId));
                }

                if (resultTypeId != null) {
                    predicates.add(cb.equal(trackJoin.get("resultType").get("id"), resultTypeId));
                }
            }

            // 3. Global Metadata Search (OR condition for multiple fields)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("exifData").get("cameraModel")), pattern),
                        cb.like(cb.lower(root.get("exifData").get("lens")), pattern)
                ));
            }

            query.distinct(true); // Avoid duplicates due to joins
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}