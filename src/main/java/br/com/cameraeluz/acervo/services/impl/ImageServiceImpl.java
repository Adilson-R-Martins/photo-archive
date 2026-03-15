package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.services.ImageService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of {@link ImageService} using the Thumbnailator library.
 *
 * <p>Generates a web-optimised JPEG (max 1280×1280 px, quality 0.8) alongside
 * each uploaded original. The web-optimised file is written to a
 * {@code thumbnails/} sibling directory at the same date level as the
 * original ({@code {year}/{month}/thumbnails/}).</p>
 */
@Service
public class ImageServiceImpl implements ImageService {

    private final Path baseStorageLocation;

    /**
     * Initialises the service with the configured upload directory.
     *
     * @param uploadDir the value of {@code photoarchive.app.upload-dir};
     *                  resolved to an absolute, normalised path at startup.
     */
    public ImageServiceImpl(@Value("${photoarchive.app.upload-dir}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Generates a web-optimised version of the given original file and returns
     * its relative path for storage in the database.
     *
     * <p>Only the {@code photos} directory segment is replaced with
     * {@code thumbnails} to derive the output path — this avoids corrupting
     * titles or filenames that happen to contain the word "photos".</p>
     *
     * @param originalRelativePath the relative path of the source file as returned
     *                             by {@link br.com.cameraeluz.acervo.services.FileStorageService#storeFile}.
     * @return the relative path of the generated web-optimised file
     *         (e.g., {@code 2024/11/thumbnails/uuid_photo.jpg}).
     * @throws RuntimeException if Thumbnailator fails to process the source image.
     */
    @Override
    public String generateWebOptimizedVersion(String originalRelativePath) {
        Path originalFile = this.baseStorageLocation.resolve(originalRelativePath);

        // Replace only the "photos" directory segment with "thumbnails" to derive the
        // output path without risking corruption of filenames that contain the word "photos".
        Path relativePath = Paths.get(originalRelativePath);
        Path parent = relativePath.getParent();           // e.g., "2024/11/photos"
        Path grandParent = parent.getParent();            // e.g., "2024/11"
        Path thumbParent = grandParent.resolve("thumbnails"); // e.g., "2024/11/thumbnails"
        Path thumbnailRelativePath = thumbParent.resolve(relativePath.getFileName()); // e.g., "2024/11/thumbnails/uuid_photo.jpg"

        Path thumbnailFile = this.baseStorageLocation.resolve(thumbnailRelativePath);

        try {
            Files.createDirectories(thumbnailFile.getParent());

            Thumbnails.of(originalFile.toFile())
                    .size(1280, 1280)
                    .outputQuality(0.8)
                    .toFile(thumbnailFile.toFile());

            return thumbnailRelativePath.toString().replace("\\", "/");
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to generate web-optimized version for '" + originalRelativePath
                    + "'. Verify the source file is a valid image in a supported format.", ex);
        }
    }
}
