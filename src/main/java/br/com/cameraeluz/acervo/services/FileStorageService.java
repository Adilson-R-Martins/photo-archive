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
     * @param file The multipart file received from the client
     * @return The unique generated filename generated for the stored file
     */
    String storeFile(MultipartFile file);

    /**
     * Loads a file from the storage as a Resource object.
     * * @param fileName The unique name of the file
     * @param isOptimized Boolean to indicate if we are looking in the optimized folder or original folder
     * @return The file as a Resource
     */
    Resource loadFileAsResource(String fileName, boolean isOptimized);
}