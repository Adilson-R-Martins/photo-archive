package br.com.cameraeluz.acervo.exceptions;

/**
 * Thrown when a create or update operation conflicts with an existing resource
 * (e.g., duplicate username or e-mail address on registration).
 *
 * <p>Maps to {@code 409 Conflict} in {@link GlobalExceptionHandler}.</p>
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
