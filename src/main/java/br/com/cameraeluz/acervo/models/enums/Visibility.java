package br.com.cameraeluz.acervo.models.enums;

/**
 * Three-tier access policy for a {@link br.com.cameraeluz.acervo.models.Photo}.
 *
 * <p>Visibility controls who may view the web-optimised photo and whether it
 * appears in search/listing results. It is orthogonal to the download permission
 * system: an {@code OPEN} photo can still require an explicit
 * {@link br.com.cameraeluz.acervo.models.DownloadPermission} for its original file.</p>
 *
 * <ul>
 *   <li>{@link #OPEN} — accessible without any authentication.
 *       Use for portfolio-style public showcases.</li>
 *   <li>{@link #PUBLIC} — accessible to any authenticated user (any role).
 *       The default for photos migrated from before this feature was introduced.</li>
 *   <li>{@link #PRIVATE} — accessible only to the uploading author, ADMIN, and EDITOR.
 *       <strong>This is the default for all new uploads</strong>, following the
 *       Principle of Least Privilege and Privacy by Design (GDPR Principle 3).</li>
 * </ul>
 *
 * <p><strong>Enforcement points:</strong>
 * <ol>
 *   <li>{@code GET /api/photos/view/**} — application-layer check in
 *       {@link br.com.cameraeluz.acervo.controllers.PhotoController#viewPhoto},
 *       returns {@code 404} for inaccessible photos (avoids leaking existence).</li>
 *   <li>{@code GET /api/photos/{id}} — application-layer check in the controller,
 *       returns {@code 403} for authenticated callers who lack visibility.</li>
 *   <li>{@code GET /api/photos} and {@code GET /api/photos/search} — filtered at the
 *       query layer via
 *       {@link br.com.cameraeluz.acervo.repositories.specs.PhotoSpecifications}.</li>
 * </ol>
 * </p>
 */
public enum Visibility {

    /**
     * No authentication required. The photo appears in public listings and its
     * view URL is accessible to anonymous HTTP clients.
     */
    OPEN,

    /**
     * Any authenticated user may view the photo regardless of role.
     * The photo appears in search results for all authenticated callers.
     */
    PUBLIC,

    /**
     * Access restricted to the uploading author, ADMIN, and EDITOR.
     * The photo is hidden from other users' search results and its view URL
     * returns {@code 404} to prevent disclosure of its existence.
     *
     * <p>This is the default for all new uploads.</p>
     */
    PRIVATE
}
