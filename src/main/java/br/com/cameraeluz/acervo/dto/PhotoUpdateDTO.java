package br.com.cameraeluz.acervo.dto;

import lombok.Data;

import java.util.Set;

@Data
public class PhotoUpdateDTO {

    private String title;
    private String artisticAuthorName;
    private Set<Long> categoryIds;
    private Boolean active; // Use Boolean para aceitar nulo caso não queira mudar o status
}