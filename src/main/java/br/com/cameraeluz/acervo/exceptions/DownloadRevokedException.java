package br.com.cameraeluz.acervo.exceptions;

/**
 * Thrown when a user attempts to download a photo whose permission has been explicitly revoked.
 */
public class DownloadRevokedException extends RuntimeException {

    public DownloadRevokedException(String message) {
        super(message);
    }
}
