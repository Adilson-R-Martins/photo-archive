package br.com.cameraeluz.acervo.repositories.specs;

import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.PhotoEventTrack;
import br.com.cameraeluz.acervo.models.enums.Visibility;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic query builder for advanced photo search.
 *
 * <p>Combines exact filters (author, event, result type), a free-text keyword
 * search across title and EXIF/IPTC metadata fields, and a visibility predicate
 * that scopes results to photos the caller is permitted to see.</p>
 *
 * <p>All filters are optional and are composed with a logical AND; the keyword
 * search uses a logical OR across all indexed text columns; the visibility
 * predicate uses a logical OR across the permitted tiers.</p>
 *
 * <h2>Visibility filtering rules</h2>
 * <ul>
 *   <li><strong>Privileged callers</strong> (ADMIN, EDITOR): no visibility
 *       restriction — all photos regardless of tier are returned to support
 *       content moderation workflows.</li>
 *   <li><strong>Regular authenticated callers</strong> ({@code callerId != null}):
 *       {@link Visibility#OPEN}, {@link Visibility#PUBLIC}, and
 *       {@link Visibility#PRIVATE} photos uploaded by the caller.</li>
 *   <li><strong>Unauthenticated callers</strong> ({@code callerId == null},
 *       {@code isPrivileged == false}): only {@link Visibility#OPEN} photos.
 *       This path is included for completeness; listing endpoints currently
 *       require authentication at the filter-chain level (Option A).</li>
 * </ul>
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
     * @param callerId     the authenticated caller's user id; {@code null} for unauthenticated callers.
     * @param isPrivileged {@code true} if the caller holds the ADMIN or EDITOR role, bypassing
     *                     all visibility restrictions to support content moderation.
     * @return a dynamic {@link Specification} ready to be passed to a JPA repository.
     */
    public static Specification<Photo> withAdvancedFilters(
            Long authorId, Long eventId, Long resultTypeId, String keyword,
            Long callerId, boolean isPrivileged) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only active (non-soft-deleted) photos are ever surfaced.
            predicates.add(cb.isTrue(root.get("active")));

            // 1. Author Filter
            if (authorId != null) {
                predicates.add(cb.equal(root.get("uploadedBy").get("id"), authorId));
            }

            // 2. Traceability (Event & Results)
            // INNER JOIN is intentional: when filtering by eventId or resultTypeId the caller
            // explicitly requests only photos that have a matching track, so photos without
            // any event participation should be excluded. Using LEFT JOIN + an equality
            // predicate on the joined table silently produces the same INNER JOIN semantics
            // in SQL but obscures the intent — making it explicit avoids future confusion.
            if (eventId != null || resultTypeId != null) {
                Join<Photo, PhotoEventTrack> trackJoin = root.join("eventTracks", JoinType.INNER);

                if (eventId != null) {
                    predicates.add(cb.equal(trackJoin.get("event").get("id"), eventId));
                }

                if (resultTypeId != null) {
                    predicates.add(cb.equal(trackJoin.get("resultType").get("id"), resultTypeId));
                }
            }

            // 3. Metadata Keywords (Title and Technical Data)
            if (keyword != null && !keyword.trim().isEmpty()) {
                // Truncate to prevent oversized patterns from scanning all EXIF columns.
                String normalized = keyword.trim();
                if (normalized.length() > 200) {
                    normalized = normalized.substring(0, 200);
                }

                // Escape LIKE metacharacters so user input is treated as a literal string.
                // Without escaping, a keyword like "%_%" would match every row.
                String escaped = normalized.toLowerCase()
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                char escapeChar = '\\';

                // ExifData fields only
                String[] exifFields = {"cameraModel", "lens", "aperture", "shutterSpeed", "iso",
                        "focalLength", "captureDate", "description", "keywords", "software", "title"};

                List<Predicate> orPredicates = new ArrayList<>();

                // Match against the photo's primary title.
                orPredicates.add(cb.like(cb.lower(root.get("title")), pattern, escapeChar));

                // Match against EXIF/IPTC metadata fields.
                for (String field : exifFields) {
                    orPredicates.add(cb.like(cb.lower(root.get("exifData").get(field)), pattern, escapeChar));
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            // 4. Visibility Filter
            // Privileged callers (ADMIN, EDITOR) bypass this predicate entirely so they
            // can moderate content regardless of visibility tier. For everyone else, only
            // photos the caller is permitted to see are included in the result set.
            if (!isPrivileged) {
                List<Predicate> visibilityOr = new ArrayList<>();

                // OPEN photos are visible to everyone, including unauthenticated callers.
                visibilityOr.add(cb.equal(root.get("visibility"), Visibility.OPEN));

                if (callerId != null) {
                    // PUBLIC photos are visible to any authenticated user.
                    visibilityOr.add(cb.equal(root.get("visibility"), Visibility.PUBLIC));

                    // PRIVATE photos are visible only to the uploading author.
                    visibilityOr.add(cb.and(
                            cb.equal(root.get("visibility"), Visibility.PRIVATE),
                            cb.equal(root.get("uploadedBy").get("id"), callerId)
                    ));
                }

                predicates.add(cb.or(visibilityOr.toArray(new Predicate[0])));
            }

            // Prevents duplicate Photo rows when a photo has multiple event tracks.
            // Only needed when the INNER JOIN on eventTracks is active; applying DISTINCT
            // unconditionally causes COUNT(DISTINCT) issues in the pagination count query
            // with Hibernate 7 when there are no joins.
            if (query != null && (eventId != null || resultTypeId != null)) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
