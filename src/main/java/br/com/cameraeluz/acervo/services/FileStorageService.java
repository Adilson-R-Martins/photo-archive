package br.com.cameraeluz.acervo.services;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface defining the contract for file storage operations.
 */
public interface FileStorageService {

    /**
     * Stores a file in the configured storage location.
     *
     * @param file The multipart file received from the client
     * @return The unique generated filename generated for the stored file
     */
    String storeFile(MultipartFile file);
}