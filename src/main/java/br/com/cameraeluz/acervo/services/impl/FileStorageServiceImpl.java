package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.exceptions.FileStorageException;
import br.com.cameraeluz.acervo.services.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * File-system implementation of {@link FileStorageService}.
 *
 * <p>Files are stored under a configurable root directory (property
 * {@code photoarchive.app.upload-dir}) in a date-partitioned hierarchy:
 * {@code {year}/{month}/photos/}. Each file is prefixed with a random UUID
 * to prevent name collisions. A parallel {@code thumbnails/} directory at the
 * same date level is managed by the image service.</p>
 *
 * <p>Path-traversal attacks are prevented by verifying that every resolved
 * path starts with the configured base directory before any I/O is performed.</p>
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path baseStorageLocation;

    /**
     * Initialises the service with the configured upload directory.
     *
     * @param uploadDir the value of {@code photoarchive.app.upload-dir};
     *                  resolved to an absolute, normalised path at startup.
     */
    public FileStorageServiceImpl(@Value("${photoarchive.app.upload-dir}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Stores a multipart file under a date-partitioned sub-directory and returns
     * the relative path (using forward slashes) for later retrieval.
     *
     * @param file  the uploaded file to persist.
     * @param title the display title associated with the file (currently unused in
     *              path generation but reserved for future naming strategies).
     * @return the relative storage path (e.g., {@code 2024/11/photos/uuid_photo.jpg}).
     * @throws FileStorageException if the file cannot be written to disk.
     */
    @Override
    public String storeFile(MultipartFile file, String title) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        Path relativeFolder = Paths.get(year, month, "photos");
        Path fullPath = this.baseStorageLocation.resolve(relativeFolder);

        try {
            Files.createDirectories(fullPath);

            // Strip path separators and unsafe characters to prevent path traversal.
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "file";
            String safeName = Paths.get(originalName).getFileName().toString()
                    .replaceAll("[^a-zA-Z0-9._-]", "_");

            String fileName = UUID.randomUUID() + "_" + safeName;
            Files.copy(file.getInputStream(), fullPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            return relativeFolder.resolve(fileName).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new FileStorageException(
                    "Failed to store file '" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown")
                    + "'. Verify the upload directory is accessible and has sufficient space.", ex);
        }
    }

    /**
     * Loads a previously stored file as a Spring {@link Resource}.
     *
     * @param relativePath the relative path returned by {@link #storeFile}.
     * @return a readable {@link Resource} pointing to the file.
     * @throws FileStorageException if the path escapes the permitted directory,
     *         if the file is not found or not readable, or if the path is malformed.
     */
    @Override
    public Resource loadFileAsResource(String relativePath) {
        try {
            Path filePath = this.baseStorageLocation.resolve(relativePath).normalize();

            if (!filePath.startsWith(this.baseStorageLocation)) {
                throw new FileStorageException(
                        "Access denied: the resolved path escapes the permitted storage directory. "
                        + "Possible path traversal attempt.");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException(
                        "File '" + relativePath + "' could not be found or is not readable. "
                        + "The resource may have been moved or deleted.");
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException(
                    "Malformed path '" + relativePath + "'. Ensure the stored path is a valid URI.", ex);
        }
    }
}
