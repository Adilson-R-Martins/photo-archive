package br.com.cameraeluz.acervo.services;

/**
 * Service interface for image processing and optimization.
 */
public interface ImageService {

    /**
     * Generates a web-optimized, lower-resolution version of an existing image.
     *
     * @param originalFileName The name of the original high-resolution file stored on disk
     * @return The generated filename of the web-optimized image
     */
    String generateWebOptimizedVersion(String originalFileName);
}