package br.com.cameraeluz.acervo.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface defining the contract for file storage operations.
 */
public interface FileStorageService {

    /**
     * Stores a file in the configured storage location.
     *
     * @param file  The multipart file received from the client
     * @param title The title of the photo to help in naming (optional logic)
     * @return The unique generated filename
     */
    String storeFile(MultipartFile file, String title);

    /**
     * Loads a file from the storage as a Resource object.
     *
     * @param fileName The name of the file to load
     * @return The file as a Resource
     */
    Resource loadFileAsResource(String fileName);
}