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
 * Dynamic query builder for advanced photo search.
 *
 * <p>Combines exact filters (author, event, result type) with a free-text keyword
 * search that spans the photo's primary title and all EXIF/IPTC metadata fields.
 * All filters are optional and are composed with a logical AND; the keyword search
 * uses a logical OR across all indexed text columns.</p>
 */
public class PhotoSpecifications {

    /**
     * Builds a combined {@link Specification} based on the provided filters.
     *
     * <p>Only active photos are ever included in results. Filters that are
     * {@code null} are silently ignored, making every parameter optional.</p>
     *
     * @param authorId     restricts results to photos uploaded by the user with this id.
     * @param eventId      restricts results to photos that participated in this event.
     * @param resultTypeId restricts results to photos that achieved this result type.
     * @param keyword      free-text search applied to the photo title and all EXIF/IPTC fields.
     * @return a dynamic {@link Specification} ready to be passed to a JPA repository.
     */
    public static Specification<Photo> withAdvancedFilters(Long authorId, Long eventId, Long resultTypeId, String keyword) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            // 1. Author Filter
            if (authorId != null) {
                predicates.add(cb.equal(root.get("uploadedBy").get("id"), authorId));
            }

            // 2. Traceability (Event & Results) - Uses LEFT JOIN to not exclude photos without events
            if (eventId != null || resultTypeId != null) {
                Join<Photo, PhotoEventTrack> trackJoin = root.join("eventTracks", JoinType.LEFT);

                if (eventId != null) {
                    predicates.add(cb.equal(trackJoin.get("event").get("id"), eventId));
                }

                if (resultTypeId != null) {
                    predicates.add(cb.equal(trackJoin.get("resultType").get("id"), resultTypeId));
                }
            }

            // 3. Metadata Keywords (Title and Technical Data)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";

                // ExifData fields only
                String[] exifFields = {"cameraModel", "lens", "aperture", "shutterSpeed", "iso",
                        "focalLength", "captureDate", "description", "keywords", "software", "title"};

                List<Predicate> orPredicates = new ArrayList<>();

                // Match against the photo's primary title.
                orPredicates.add(cb.like(cb.lower(root.get("title")), pattern));

                // Match against EXIF/IPTC metadata fields.
                for (String field : exifFields) {
                    orPredicates.add(cb.like(cb.lower(root.get("exifData").get(field)), pattern));
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            // Prevents duplicate Photo entries in results when a photo has multiple event tracks
            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
