package br.com.cameraeluz.acervo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class PhotoUpdateDTO {

    @NotBlank(message = "O título não pode ser vazio")
    private String title;
    private String artisticAuthorName;
    @NotBlank(message = "A categoria não pode ser vazia")
    private Set<Long> categoryIds;
    private Boolean active; // Use Boolean para aceitar nulo caso não queira mudar o status
}