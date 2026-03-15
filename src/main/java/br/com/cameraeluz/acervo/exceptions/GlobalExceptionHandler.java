package br.com.cameraeluz.acervo.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p>Maps application exceptions to structured {@link StandardError} responses
 * with appropriate HTTP status codes. Each handler documents its covered
 * exception type and the HTTP status it produces.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles entity-not-found errors thrown when a database lookup returns no result.
     *
     * @param ex      the exception carrying the actionable message.
     * @param request the current HTTP request (available for future path logging).
     * @return {@code 404 Not Found} with the exception message as the error detail.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<StandardError> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    /**
     * Handles bean-validation failures produced by {@code @Valid} on request bodies.
     *
     * @param ex      the exception containing the list of field-level violations.
     * @param request the current HTTP request.
     * @return {@code 400 Bad Request} with all violation messages in the {@code details} list.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "One or more request fields failed validation. See 'details' for the list of violations.",
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    /**
     * Handles image-processing failures thrown by the thumbnail/optimisation pipeline
     * (e.g., a corrupted source file or an unsupported image encoding).
     *
     * <p>The real cause is logged server-side; the client receives a safe 422 message
     * that does not reveal internal library details or file-system paths.</p>
     *
     * @param ex      the image processing exception.
     * @param request the current HTTP request.
     * @return {@code 422 Unprocessable Entity}.
     */
    @ExceptionHandler(ImageProcessingException.class)
    public ResponseEntity<StandardError> handleImageProcessing(
            ImageProcessingException ex, HttpServletRequest request) {
        logger.error("Image processing failed at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Image Processing Error",
                "The uploaded file could not be processed. Ensure it is a valid, uncorrupted image.",
                null
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(err);
    }

    /**
     * Handles file-storage errors thrown by the storage layer (e.g., write failure,
     * path-traversal attempt, or file-not-found on retrieval).
     *
     * @param ex      the storage exception.
     * @param request the current HTTP request.
     * @return {@code 500 Internal Server Error} with the exception message.
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<StandardError> handleFileStorageException(
            FileStorageException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Storage Error",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    /**
     * Handles invalid-argument and invalid-state errors (e.g., unsupported media type,
     * inactive photo submitted to an event).
     *
     * @param ex      the runtime exception.
     * @param request the current HTTP request.
     * @return {@code 400 Bad Request} with the exception message.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<StandardError> handleIllegalArgument(
            RuntimeException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    /**
     * Handles explicitly revoked download permissions.
     *
     * @param ex      the exception thrown when a permission record has been revoked.
     * @param request the current HTTP request.
     * @return {@code 403 Forbidden} with the exception message.
     */
    @ExceptionHandler(DownloadRevokedException.class)
    public ResponseEntity<StandardError> handleDownloadRevoked(
            DownloadRevokedException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Download Revoked",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    /**
     * Handles exhausted download limits for a given permission record.
     *
     * @param ex      the exception thrown when the download counter reaches its maximum.
     * @param request the current HTTP request.
     * @return {@code 429 Too Many Requests} with the exception message.
     */
    @ExceptionHandler(DownloadLimitReachedException.class)
    public ResponseEntity<StandardError> handleDownloadLimitReached(
            DownloadLimitReachedException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Download Limit Reached",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(err);
    }

    /**
     * Handles access-denied errors thrown when the caller lacks the required
     * role or ownership to perform the requested operation.
     *
     * @param ex      the Spring Security access-denied exception.
     * @param request the current HTTP request.
     * @return {@code 403 Forbidden} with the exception message.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    /**
     * Handles resource-conflict errors (e.g., duplicate username or e-mail on registration).
     *
     * @param ex      the conflict exception.
     * @param request the current HTTP request.
     * @return {@code 409 Conflict} with the exception message.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardError> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    /**
     * Fallback handler for any unclassified runtime exception not covered by a
     * more specific handler above.
     *
     * <p>The real exception message is logged server-side only — never forwarded to the
     * client — to prevent leaking internal implementation details (SQL error text,
     * Hibernate class names, file-system paths, etc.).</p>
     *
     * @param ex      the unhandled runtime exception.
     * @param request the current HTTP request.
     * @return {@code 500 Internal Server Error} with a generic, safe message.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardError> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        logger.error("Unhandled exception at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        StandardError err = new StandardError(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact support if the problem persists.",
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
