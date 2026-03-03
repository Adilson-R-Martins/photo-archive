package br.com.cameraeluz.acervo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class ExifData {
    private String cameraModel;
    private String lens;
    private String focalLength;
    private String aperture;
    private String shutterSpeed;
    private String iso;
    private String captureDate;

    // IPTC / Software
    private String software; // Ex: Adobe Photoshop Lightroom
    private String copyright; // Ex: © Adilson Martins

    @Column(columnDefinition = "TEXT")
    private String keywords; // Ex: Natureza, Pássaro, Macro (IPTC)

    @Column(columnDefinition = "TEXT")
    private String description; // Novo campo para IPTC Description/Caption
}