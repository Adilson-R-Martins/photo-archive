package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.exceptions.FileStorageException;
import br.com.cameraeluz.acervo.services.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

/**
 * Implementation of the FileStorageService handling local file system storage.
 * Ensures directory creation, file name uniqueness (UUID), and security validation.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private final Path optimizedStorageLocation;

    public FileStorageServiceImpl(
            @Value("${photoarchive.app.upload-dir}") String uploadDir,
            @Value("${photoarchive.app.optimized-dir}") String optimizedDir) {

        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.optimizedStorageLocation = Paths.get(optimizedDir).toAbsolutePath().normalize();
    }

    /**
     * Initializes the storage directories on application startup.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.optimizedStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directories where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // Clean the file name to prevent directory traversal
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            // Security Check: Block directory traversal attacks
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence: " + originalFileName);
            }

            // Generate a unique filename using UUID to prevent overwriting
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Resolve the target path and copy the file (replacing existing if somehow UUID crashes)
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return uniqueFileName;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName, boolean isOptimized) {
        try {
            Path filePath = isOptimized
                    ? Paths.get(this.optimizedStorageLocation.toString()).resolve(fileName).normalize()
                    : this.fileStorageLocation.resolve(fileName).normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + fileName, ex);
        }
    }
}