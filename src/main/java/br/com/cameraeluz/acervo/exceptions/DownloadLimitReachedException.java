package br.com.cameraeluz.acervo.exceptions;

/**
 * Thrown when a user attempts to download a photo beyond their permitted download limit.
 */
public class DownloadLimitReachedException extends RuntimeException {

    public DownloadLimitReachedException(String message) {
        super(message);
    }
}
