package br.com.cameraeluz.acervo.exceptions;

/**
 * Custom exception to handle errors related to file storage operations.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}