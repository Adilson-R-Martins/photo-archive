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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<PhotoResponseDTO> searchPhotos(Long authorId, Long eventId, Long resultTypeId, String keyword) {
        Specification<Photo> spec = PhotoSpecifications.withAdvancedFilters(authorId, eventId, resultTypeId, keyword);
        return photoRepository.findAll(spec).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PhotoResponseDTO convertToDTO(Photo photo) {
        PhotoResponseDTO dto = new PhotoResponseDTO();
        dto.setId(photo.getId());
        dto.setTitle(photo.getTitle());
        dto.setArtisticAuthorName(photo.getArtisticAuthorName());
        dto.setMetadata(photo.getExifData());

        dto.setViewUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/view/").path(photo.getWebOptimizedPath()).toUriString());

        dto.setDownloadUrl(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/photos/download/").path(photo.getStoragePath()).toUriString());

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


    @Transactional(readOnly = true)
    public boolean isOwner(String username, Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Foto não encontrada"));
        return photo.getUploadedBy().getUsername().equals(username);
    }

    @Transactional
    public PhotoResponseDTO uploadPhoto(MultipartFile file, String title, Set<Long> categoryIds) {
        validateFileType(file);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + username));

        ExifData exifData = metadataService.extractMetadata(file);
        String originalPath = fileStorageService.storeFile(file, title);
        String webPath = imageService.generateWebOptimizedVersion(originalPath);

        Set<Category> categories = categoryIds.stream()
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada ID: " + id)))
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

    @Transactional
    public PhotoResponseDTO uploadPhoto(MultipartFile file, String title, String artisticName, Set<Long> categoryIds) {
        validateFileType(file);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        ExifData exifData = metadataService.extractMetadata(file);
        String originalPath = fileStorageService.storeFile(file, title);
        String webPath = imageService.generateWebOptimizedVersion(originalPath);

        Set<Category> categories = categoryIds.stream()
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Cat ID: " + id)))
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
     * Validates that the uploaded file is an allowed image type.
     * Checks the Content-Type declared by the client as a first filter.
     * NOTE: for stronger guarantees, replace with Apache Tika magic-byte inspection.
     */
    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Envie uma imagem JPEG, PNG, TIFF ou WebP.");
        }
    }

    @Transactional
    public PhotoResponseDTO updatePhoto(Long id, PhotoUpdateDTO dto) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Foto não encontrada"));

        if (dto.getTitle() != null) photo.setTitle(dto.getTitle());
        if (dto.getArtisticAuthorName() != null) photo.setArtisticAuthorName(dto.getArtisticAuthorName());
        if (dto.getActive() != null) photo.setActive(dto.getActive());

        if (dto.getCategoryIds() != null) {
            Set<Category> categories = dto.getCategoryIds().stream()
                    .map(catId -> categoryRepository.findById(catId)
                            .orElseThrow(() -> new EntityNotFoundException("Cat ID: " + catId)))
                    .collect(Collectors.toSet());
            photo.setCategories(categories);
        }

        return convertToDTO(photoRepository.save(photo));
    }
}
