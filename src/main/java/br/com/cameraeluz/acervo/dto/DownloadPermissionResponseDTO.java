package br.com.cameraeluz.acervo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection returned after granting, revoking, or listing download permissions.
 */
@Getter
@Builder
@AllArgsConstructor
public class DownloadPermissionResponseDTO {

    private UUID id;

    private Long userId;
    private String username;

    private Long photoId;
    private String photoTitle;

    private int downloadCount;
    private int downloadLimit;

    private boolean revoked;

    private LocalDateTime grantedAt;
    private Long grantedByUserId;
}
