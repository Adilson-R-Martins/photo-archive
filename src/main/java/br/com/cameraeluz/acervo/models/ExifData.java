package br.com.cameraeluz.acervo.models;

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
}