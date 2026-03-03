package br.com.cameraeluz.acervo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Set;

/**
 * Data Transfer Object for Photo entities.
 * Flattens the photo data for gallery display and hides sensitive information.
 */
@Data
@AllArgsConstructor
public class PhotoResponseDTO {
    private Long id;
    private String title;
    private String artisticAuthorName;
    private Set<String> categories; // Apenas os nomes das categorias
    private String viewUrl;         // URL completa para a tag <img>
    private String downloadUrl;     // URL completa para o botão de download
    private String cameraModel;     // Resumo do EXIF
    private String captureDate;
}