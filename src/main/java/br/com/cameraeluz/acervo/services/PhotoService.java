package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.ExifDataDTO;
import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.dto.PhotoUpdateDTO;
import br.com.cameraeluz.acervo.models.Category;
import br.com.cameraeluz.acervo.models.ExifData;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.models.enums.Visibility;
import br.com.cameraeluz.acervo.repositories.CategoryRepository;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import br.com.cameraeluz.acervo.repositories.specs.PhotoSpecifications;
import br.com.cameraeluz.acervo.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.tika.Tika;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core service for managing photos in the archive.
 *
 * <p>Handles the full lifecycle of a photo: file validation, storage,
 * web-optimized version generation, EXIF/IPTC metadata extraction,
 * category assignment, visibility-aware search, and soft deletion.</p>
 *
 * <h2>Visibility enforcement</h2>
 * <p>Every read path enforces the three-tier {@link Visibility} policy:</p>
 * <ul>
 *   <li>{@link #searchPhotos} — filters results at the query layer via
 *       {@link PhotoSpecifications}.</li>
 *   <li>{@link #isVisibleTo} — evaluated by controllers for single-photo
 *       endpoints ({@code /view/**} and {@code /{id}}).</li>
 * </ul>
 *
 * <p>Write paths ({@link #uploadPhoto}, {@link #updatePhoto}) accept an
 * explicit {@link Visibility} value so callers can set the tier at
 * creation/update time. New uploads default to {@link Visibility#PRIVATE}.</p>
 */
@Service
@RequiredArgsConstructor
public class PhotoService {

    @Value("${photoarchive.app.base-url}")
    private String baseUrl;

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final ImageService imageService;
    private final MetadataService metadataService;

    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/tiff", "image/webp");

    private static final Tika TIKA = new Tika();

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Searches photos using optional filters combined with free-text keyword
     * search, scoped to photos the caller is permitted to see.
     *
     * <p>Visibility rules applied per caller context:</p>
     * <ul>
     *   <li>ADMIN / EDITOR — see all photos (full moderation access).</li>
     *   <li>Regular authenticated users — see {@link Visibility#OPEN},
     *       {@link Visibility#PUBLIC}, and their own {@link Visibility#PRIVATE}
     *       photos.</li>
     *   <li>Unauthenticated callers — see only {@link Visibility#OPEN} photos
     *       (included for completeness; listing endpoints currently require auth).</li>
     * </ul>
     *
     * @param authorId     optional filter by the user who uploaded the photo.
     * @param eventId      optional filter by participation in a specific event.
     * @param resultTypeId optional filter by a specific award or result type.
     * @param keyword      optional free-text search across title and EXIF/IPTC fields.
     * @param pageable     pagination and sorting parameters.
     * @param authentication the caller's authentication context; used to derive
     *                       visibility scope.
     * @return a paginated list of {@link PhotoResponseDTO} matching the filters
     *         and permitted by the caller's visibility scope.
     */
    @Transactional(readOnly = true)
    public Page<PhotoResponseDTO> searchPhotos(
            Long authorId,
            Long eventId,
            Long resultTypeId,
            String keyword,
            Pageable pageable,
            Authentication authentication) {

        Long callerId = null;
        boolean isPrivileged = false;

        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (authenticated) {
            Collection<String> roles = SecurityUtils.extractRoles(authentication);
            isPrivileged = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EDITOR");
            if (!isPrivileged) {
                callerId = SecurityUtils.getUserDetails(authentication).getId();
            }
        }

        Specification<Photo> spec = PhotoSpecifications
                .withAdvancedFilters(authorId, eventId, resultTypeId, keyword, callerId, isPrivileged);

        return photoRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    /**
     * Determines whether the given caller may view the supplied photo based on
     * its {@link Visibility} tier.
     *
     * <p>Visibility precedence (evaluated top-to-bottom, first match wins):</p>
     * <ol>
     *   <li>{@link Visibility#OPEN} — always accessible, even without authentication.</li>
     *   <li>ADMIN / EDITOR — always have full access for content moderation.</li>
     *   <li>{@link Visibility#PUBLIC} — accessible to any authenticated user.</li>
     *   <li>{@link Visibility#PRIVATE} — accessible only to the photo's uploader.</li>
     * </ol>
     *
     * <p>This method is designed to be called from controllers on already-loaded
     * {@link Photo} entities. The {@code uploadedBy} association is eagerly fetched
     * by the repository ({@code @EntityGraph}) so it is safe to access on a detached
     * entity.</p>
     *
     * @param photo          the photo whose visibility is evaluated.
     * @param authentication the caller's authentication context; may be {@code null}
     *                       or an {@link AnonymousAuthenticationToken} for unauthenticated
     *                       requests (the view endpoint is {@code permitAll}).
     * @return {@code true} if the caller is permitted to view this photo.
     */
    public boolean isVisibleTo(Photo photo, Authentication authentication) {
        // OPEN photos are always accessible regardless of authentication state.
        if (photo.getVisibility() == Visibility.OPEN) {
            return true;
        }

        // Treat anonymous tokens the same as a missing authentication object.
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!authenticated) {
            return false;
        }

        // Privileged roles (ADMIN, EDITOR) bypass all visibility restrictions.
        Collection<String> roles = SecurityUtils.extractRoles(authentication);
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EDITOR")) {
            return true;
        }

        // PUBLIC photos are accessible to any authenticated user.
        if (photo.getVisibility() == Visibility.PUBLIC) {
            return true;
        }

        // PRIVATE: only the uploading author may access.
        // uploadedBy is eagerly fetched by the repository (@EntityGraph) so this
        // access is safe on a detached entity.
        Long callerId = SecurityUtils.getUserDetails(authentication).getId();
        return photo.getUploadedBy().getId().equals(callerId);
    }

    /**
     * Converts a {@link Photo} entity to a {@link PhotoResponseDTO}, including
     * view/download URLs, category names, event history, and current visibility.
     *
     * @param photo the photo entity to convert.
     * @return the populated response DTO.
     */
    public PhotoResponseDTO convertToDTO(Photo photo) {
        PhotoResponseDTO dto = new PhotoResponseDTO();
        dto.setId(photo.getId());
        dto.setTitle(photo.getTitle());
        dto.setArtisticAuthorName(photo.getArtisticAuthorName());
        dto.setVisibility(photo.getVisibility());
        dto.setMetadata(toExifDataDTO(photo.getExifData()));

        dto.setViewUrl(baseUrl + "/api/photos/view/" + photo.getWebOptimizedPath());
        dto.setDownloadUrl(baseUrl + "/api/photos/download/" + photo.getId());

        dto.setCategories(photo.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet()));

        if (photo.getEventTracks() != null) {
            dto.setEventHistory(photo.getEventTracks().stream().map(track -> {
                PhotoResponseDTO.TrackInfoDTO t = new PhotoResponseDTO.TrackInfoDTO();
                t.setEventName(track.getEvent().getName());
                t.setResultDescription(track.getResultType().getDescription());
                t.setHonorReceived(track.getHonorReceived());
                t.setEventDate(track.getEvent().getEventDate().toString());
                return t;
            }).collect(Collectors.toList()));
        }
        return dto;
    }

    /**
     * Checks whether the given username is the owner (uploader) of a photo.
     *
     * @param username the username of the caller.
     * @param photoId  the id of the photo to check ownership for.
     * @return {@code true} if the caller uploaded the photo; {@code false} otherwise.
     * @throws EntityNotFoundException if no photo with the given id exists.
     */
    @Transactional(readOnly = true)
    public boolean isOwner(String username, Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Photo with id " + photoId + " was not found."));
        return photo.getUploadedBy().getUsername().equals(username);
    }

    /**
     * Retrieves a photo by its web-optimised file path.
     *
     * <p>The {@code uploadedBy} association is eagerly fetched so that
     * {@link #isVisibleTo} can be called on the returned entity without
     * risk of {@code LazyInitializationException}.</p>
     *
     * @param path the relative web-optimised path as stored in the database.
     * @return the matching {@link Photo} entity.
     * @throws EntityNotFoundException if no photo matches the given path.
     */
    @Transactional(readOnly = true)
    public Photo findByWebOptimizedPath(String path) {
        return photoRepository.findByWebOptimizedPath(path)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo was not found at the requested path."));
    }

    /**
     * Retrieves a photo by its primary key.
     *
     * <p>The {@code uploadedBy} association is eagerly fetched so that
     * {@link #isVisibleTo} can be called on the returned entity without
     * risk of {@code LazyInitializationException}.</p>
     *
     * @param id the photo id.
     * @return the matching {@link Photo} entity.
     * @throws EntityNotFoundException if no photo with the given id exists.
     */
    @Transactional(readOnly = true)
    public Photo findById(Long id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo not found with id: " + id));
    }

    /**
     * Returns the full detail of a single photo as a response DTO.
     *
     * @param id the photo id.
     * @return the photo as a {@link PhotoResponseDTO}.
     * @throws EntityNotFoundException if no photo with the given id exists.
     */
    @Transactional(readOnly = true)
    public PhotoResponseDTO getPhotoById(Long id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo not found with id: " + id));
        return convertToDTO(photo);
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Uploads a new photo, defaulting to {@link Visibility#PRIVATE}.
     *
     * <p>Delegates to {@link #uploadPhoto(MultipartFile, String, String, Visibility, Set)}
     * with {@code artisticName = null} and {@code visibility = PRIVATE}.</p>
     *
     * @param file        the image file to store.
     * @param title       the display title for the photo.
     * @param categoryIds the set of category ids to associate with the photo.
     * @return the persisted photo as a {@link PhotoResponseDTO}.
     * @throws EntityNotFoundException  if the authenticated user or any category is not found.
     * @throws IllegalArgumentException if the file's content type is not permitted.
     */
    @Transactional
    public PhotoResponseDTO uploadPhoto(MultipartFile file, String title, Set<Long> categoryIds) {
        return uploadPhoto(file, title, null, Visibility.PRIVATE, categoryIds);
    }

    /**
     * Uploads a new photo with an explicit visibility tier.
     *
     * <p>If {@code artisticName} is {@code null} or blank, the uploader's
     * profile artistic name is used as a fallback. If {@code visibility} is
     * {@code null}, it defaults to {@link Visibility#PRIVATE}.</p>
     *
     * @param file         the image file (JPEG, PNG, TIFF, or WebP).
     * @param title        the display title for the photo.
     * @param artisticName optional override for the artistic author name;
     *                     falls back to the uploader's profile artistic name.
     * @param visibility   the initial access policy; defaults to
     *                     {@link Visibility#PRIVATE} when {@code null}.
     * @param categoryIds  the set of category ids to associate with the photo.
     * @return the persisted photo as a {@link PhotoResponseDTO}.
     * @throws EntityNotFoundException  if the authenticated user or any category is not found.
     * @throws IllegalArgumentException if the file's content type is not permitted.
     */
    @Transactional
    public PhotoResponseDTO uploadPhoto(
            MultipartFile file,
            String title,
            String artisticName,
            Visibility visibility,
            Set<Long> categoryIds) {

        validateFileType(file);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User '" + username + "' was not found. Ensure the username is correct."));

        ExifData exifData = metadataService.extractMetadata(file);
        String originalPath = fileStorageService.storeFile(file, title);
        String webPath = imageService.generateWebOptimizedVersion(originalPath);

        Set<Category> categories = categoryIds.stream()
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Category with id " + id + " was not found. Provide a valid category identifier.")))
                .collect(Collectors.toSet());

        Photo photo = new Photo();
        photo.setTitle(title);
        photo.setOriginalFileName(file.getOriginalFilename());
        photo.setArtisticAuthorName((artisticName != null && !artisticName.isBlank())
                ? artisticName : user.getArtisticName());
        photo.setStoragePath(originalPath);
        photo.setWebOptimizedPath(webPath);
        photo.setExifData(exifData);
        photo.setUploadedBy(user);
        photo.setCategories(categories);
        photo.setVisibility(visibility != null ? visibility : Visibility.PRIVATE);

        return convertToDTO(photoRepository.save(photo));
    }

    /**
     * Updates mutable fields of an existing photo (title, artistic name, active flag,
     * visibility, or category set). Only non-null fields in the DTO are applied.
     *
     * @param id  the id of the photo to update.
     * @param dto the partial update data.
     * @return the updated photo as a {@link PhotoResponseDTO}.
     * @throws EntityNotFoundException if no photo or category with the given id exists.
     */
    @Transactional
    public PhotoResponseDTO updatePhoto(Long id, PhotoUpdateDTO dto) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Photo with id " + id + " was not found."));

        if (dto.getTitle() != null) photo.setTitle(dto.getTitle());
        if (dto.getArtisticAuthorName() != null) photo.setArtisticAuthorName(dto.getArtisticAuthorName());
        if (dto.getActive() != null) photo.setActive(dto.getActive());
        if (dto.getVisibility() != null) photo.setVisibility(dto.getVisibility());

        if (dto.getCategoryIds() != null) {
            Set<Category> categories = dto.getCategoryIds().stream()
                    .map(catId -> categoryRepository.findById(catId)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Category with id " + catId + " was not found. Provide a valid category identifier.")))
                    .collect(Collectors.toSet());
            photo.setCategories(categories);
        }

        return convertToDTO(photoRepository.save(photo));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private ExifDataDTO toExifDataDTO(ExifData exif) {
        if (exif == null) {
            return null;
        }
        ExifDataDTO dto = new ExifDataDTO();
        dto.setIptcTitle(exif.getTitle());
        dto.setCameraModel(exif.getCameraModel());
        dto.setLens(exif.getLens());
        dto.setFocalLength(exif.getFocalLength());
        dto.setAperture(exif.getAperture());
        dto.setShutterSpeed(exif.getShutterSpeed());
        dto.setIso(exif.getIso());
        dto.setCaptureDate(exif.getCaptureDate());
        dto.setSoftware(exif.getSoftware());
        dto.setCopyright(exif.getCopyright());
        dto.setKeywords(exif.getKeywords());
        dto.setDescription(exif.getDescription());
        return dto;
    }

    /**
     * Validates the uploaded file's actual binary content against the list of accepted image formats.
     *
     * <p>Uses Apache Tika to inspect the file's magic bytes rather than trusting the
     * client-supplied {@code Content-Type} header, which can be trivially spoofed.
     * The stream is wrapped in a {@link BufferedInputStream} to provide mark/reset
     * support required by Tika's {@code DefaultDetector} pipeline; without it,
     * files stored to disk by Tomcat (which uses {@code FileInputStream}) fall back
     * to {@code application/octet-stream}.</p>
     *
     * @param file the multipart file to inspect.
     * @throws IllegalArgumentException if the file is empty, the detected type is not
     *         in the allowed list, or the file content cannot be read.
     */
    private void validateFileType(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty. Please attach a valid image file.");
        }
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            String detected = TIKA.detect(in, file.getOriginalFilename());
            if (!ALLOWED_TYPES.contains(detected)) {
                throw new IllegalArgumentException(
                        "Unsupported media type '" + detected + "'. Accepted formats: JPEG, PNG, TIFF, WebP.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read upload content for type validation.", e);
        }
    }
}
