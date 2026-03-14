package br.com.cameraeluz.acervo.services;

import br.com.cameraeluz.acervo.models.ExifData;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service responsible for extracting technical metadata from image files.
 */
public interface MetadataService {
    /**
     * Extracts EXIF information from a provided image file.
     *
     * @param file The image file.
     * @return An ExifData object populated with technical details.
     */
    ExifData extractMetadata(MultipartFile file);
}