package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.dto.PhotoResponseDTO;
import br.com.cameraeluz.acervo.dto.PhotoUpdateDTO;
import br.com.cameraeluz.acervo.models.Category;
import br.com.cameraeluz.acervo.models.ExifData;
import br.com.cameraeluz.acervo.models.Photo;
import br.com.cameraeluz.acervo.models.User;
import br.com.cameraeluz.acervo.repositories.CategoryRepository;
import br.com.cameraeluz.acervo.repositories.PhotoRepository;
import br.com.cameraeluz.acervo.repositories.UserRepository;
import br.com.cameraeluz.acervo.repositories.specs.PhotoSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core service for managing photos in the archive.
 *
 * <p>Handles the full lifecycle of a photo: file validation, storage,
 * web-optimized version generation, EXIF/IPTC metadata extraction,
 * category assignment, search with advanced filters, and soft deletion
 * via the {@link #updatePhoto} method.</p>
 */
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final ImageService imageService;
    private final MetadataService metadataService;

    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/tiff", "image/webp");

    /**
     * Searches photos using optional filters combined with free-text keyword search.
     *
     * @param authorId     optional filter by the user who uploaded the photo.
     * @param eventId      optional filter by participation in a specific event.
     * @param resultTypeId optional filter by a specific award or result type.
     * @param keyword      optional free-text search across title and EXIF/IPTC fields.
     * @param pageable     pagination and sorting parameters.
     * @return a paginated list of {@link PhotoResponseDTO} matching the filters.
     */
    @Transactional(readOnly = true)
    public Page<PhotoResponseDTO> searchPhotos(
            Long authorId,
            Long eventId,
            Long resultTypeId,
            String keyword,
            Pageable pageable) {

        Specification<Photo> spec = PhotoSpecifications
                .withAdvancedFilters(authorId, eventId, resultTypeId, keyword);

        return photoRepository.findAll(spec, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Converts a {@link Photo} entity to a {@link PhotoResponseDTO}, including
     * view/download URLs, category names, and event history.
     *
     * @param photo the photo entity to convert.
     * @return the populated response DTO.
     */
    public PhotoResponseDTO convertToDTO(Photo photo) {
        PhotoResponseDTO dto = new PhotoResponseDTO();
        dto.setId(photo.getId());
        dto.setTitle(photo.getTitle());
        dto.setArtisticAuthorName(photo.getArtisticAuthorName());
        dto.setMetadata(photo.getExifData());

        dto.setViewUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/view/").path(photo.getWebOptimizedPath()).toUriString());

        dto.setDownloadUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/download/").path(String.valueOf(photo.getId())).toUriString());

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
     * Uploads a new photo and derives the artistic author name from the uploader's profile.
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
        photo.setArtisticAuthorName(user.getArtisticName());
        photo.setStoragePath(originalPath);
        photo.setWebOptimizedPath(webPath);
        photo.setExifData(exifData);
        photo.setUploadedBy(user);
        photo.setCategories(categories);

        return convertToDTO(photoRepository.save(photo));
    }

    /**
     * Uploads a new photo with an explicit artistic author name override.
     *
     * <p>If {@code artisticName} is {@code null} or blank, the uploader's
     * profile artistic name is used as a fallback.</p>
     *
     * @param file          the image file to store.
     * @param title         the display title for the photo.
     * @param artisticName  optional override for the artistic author name.
     * @param categoryIds   the set of category ids to associate with the photo.
     * @return the persisted photo as a {@link PhotoResponseDTO}.
     * @throws EntityNotFoundException  if the authenticated user or any category is not found.
     * @throws IllegalArgumentException if the file's content type is not permitted.
     */
    @Transactional
    public PhotoResponseDTO uploadPhoto(MultipartFile file, String title, String artisticName, Set<Long> categoryIds) {
        validateFileType(file);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Authenticated user was not found in the database. Re-authenticate and try again."));

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
        photo.setActive(true);

        return convertToDTO(photoRepository.save(photo));
    }

    /**
     * Updates mutable fields of an existing photo (title, artistic name, active flag,
     * or category set). Only non-null fields in the DTO are applied.
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

    /**
     * Validates that the uploaded file's declared content type is an accepted image format.
     *
     * <p>NOTE: for stronger guarantees, replace with Apache Tika magic-byte inspection.</p>
     *
     * @param file the multipart file whose content type is checked.
     * @throws IllegalArgumentException if the content type is absent or not in the allowed list.
     */
    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported media type '" + contentType + "'. Accepted formats are JPEG, PNG, TIFF, and WebP.");
        }
    }
}
