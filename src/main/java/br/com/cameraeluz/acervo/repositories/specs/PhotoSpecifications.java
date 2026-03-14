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
 * Dynamic search engine for the Photo Archive.
 * Allows combining exact filters (Author, Event, Result) with free-text search (Metadata).
 */
public class PhotoSpecifications {

    /**
     * Builds a combined query based on provided filters.
     * * @param authorId Filter by the user who uploaded the photo.
     *
     * @param eventId      Filter by participation in a specific event.
     * @param resultTypeId Filter by a specific award or result type.
     * @param keyword      Global search across titles and EXIF/IPTC metadata.
     * @return A dynamic Specification for JPA.
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

                // 1. Busca no título PRINCIPAL da Photo (root)
                orPredicates.add(cb.like(cb.lower(root.get("title")), pattern));

                // 2. Busca no título e outros campos do EXIF (root.exifData)
                for (String field : exifFields) {
                    // Importante: certifique-se que sua classe ExifData tenha o campo "title"
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