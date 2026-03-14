package br.com.cameraeluz.acervo.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class StandardError {
    private LocalDateTime timestamp;
    private Integer status;
    private String error;
    private String message;
    private List<String> details; // Usado para mostrar erros do @Valid (ex: "O nome é obrigatório")
}