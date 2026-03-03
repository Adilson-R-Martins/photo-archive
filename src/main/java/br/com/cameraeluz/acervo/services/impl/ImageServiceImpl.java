package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.exceptions.FileStorageException;
import br.com.cameraeluz.acervo.services.ImageService;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of ImageService using the Thumbnailator library.
 * Handles the resizing and compression of high-resolution image files.
 */
@Service
public class ImageServiceImpl implements ImageService {

    private final Path originalStorageLocation;
    private final Path optimizedStorageLocation;

    public ImageServiceImpl(
            @Value("${photoarchive.app.upload-dir}") String uploadDir,
            @Value("${photoarchive.app.optimized-dir}") String optimizedDir) {
        this.originalStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.optimizedStorageLocation = Paths.get(optimizedDir).toAbsolutePath().normalize();
    }

    /**
     * Initializes the optimized images directory on application startup.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.optimizedStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory for optimized images.", ex);
        }
    }

    @Override
    public String generateWebOptimizedVersion(String originalFileName) {
        try {
            // Locate the original file
            File originalFile = this.originalStorageLocation.resolve(originalFileName).toFile();

            // Define the name and path for the optimized file
            String optimizedFileName = "web_" + originalFileName;
            File optimizedFile = this.optimizedStorageLocation.resolve(optimizedFileName).toFile();

            // Process the image: resize to max 1280x1280 (maintaining aspect ratio) and 80% quality
            Thumbnails.of(originalFile)
                    .size(1280, 1280)
                    .outputQuality(0.8)
                    .toFile(optimizedFile);

            return optimizedFileName;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate web-optimized image for: " + originalFileName, ex);
        }
    }
}