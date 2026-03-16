package br.com.cameraeluz.acervo.dto;

import br.com.cameraeluz.acervo.models.enums.Visibility;
import lombok.Data;

import java.util.Set;

/**
 * Partial-update payload for {@code PUT /api/photos/{id}}.
 *
 * <p>All fields are optional. {@code null} values are ignored — only
 * non-null fields are applied to the persisted photo. This gives callers
 * fine-grained control without needing to resend the full photo state.</p>
 */
@Data
public class PhotoUpdateDTO {

    private String title;
    private String artisticAuthorName;
    private Set<Long> categoryIds;

    /** {@code null} = do not change the soft-delete flag. */
    private Boolean active;

    /**
     * New access policy to apply to the photo.
     *
     * <p>{@code null} = leave the current visibility unchanged.
     * Authors may promote their work to {@link Visibility#PUBLIC} or
     * {@link Visibility#OPEN}, or demote it back to {@link Visibility#PRIVATE}.</p>
     *
     * @see Visibility
     */
    private Visibility visibility;
}