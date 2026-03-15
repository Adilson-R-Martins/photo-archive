package br.com.cameraeluz.acervo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for granting or updating a download permission.
 */
@Getter
@Setter
@NoArgsConstructor
public class DownloadPermissionRequestDTO {

    @NotNull(message = "Target user ID is required.")
    private Long userId;

    @NotNull(message = "Photo ID is required.")
    private Long photoId;

    @Min(value = 1, message = "Download limit must be at least 1.")
    private int downloadLimit = 1;
}
