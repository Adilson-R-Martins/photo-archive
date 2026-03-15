package br.com.cameraeluz.acervo.exceptions;

/**
 * Thrown when the image processing pipeline (thumbnail generation, format conversion)
 * fails for a reason specific to the image content or the underlying library.
 *
 * <p>Extends {@link RuntimeException} so callers are not forced to declare it,
 * matching the project's unchecked-exception convention. The {@link GlobalExceptionHandler}
 * maps this to {@code 422 Unprocessable Entity} with a safe, generic client message.</p>
 */
public class ImageProcessingException extends RuntimeException {

    public ImageProcessingException(String message) {
        super(message);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
