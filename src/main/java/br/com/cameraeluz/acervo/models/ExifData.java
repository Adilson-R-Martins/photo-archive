package br.com.cameraeluz.acervo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Embeddable value object storing EXIF and IPTC metadata extracted from an image file.
 *
 * <p>EXIF fields ({@code cameraModel}, {@code lens}, {@code focalLength}, {@code aperture},
 * {@code shutterSpeed}, {@code iso}, {@code captureDate}) are populated from the standard
 * EXIF IFD0 and SubIFD directories. IPTC fields ({@code title}, {@code description},
 * {@code copyright}, {@code keywords}) are populated from the IPTC directory.
 * All fields are optional and remain {@code null} when the source file does not contain
 * the corresponding tag.</p>
 */
@Embeddable
@Getter
@Setter
public class ExifData {

    @Column(name = "exif_title")
    private String title;
    private String cameraModel;
    private String lens;
    private String focalLength;
    private String aperture;
    private String shutterSpeed;
    private String iso;
    private String captureDate;

    /**
     * The software used to process or edit the image
     * (e.g., Adobe Lightroom, Photoshop).
     */
    private String software;

    /**
     * The copyright notice embedded in the image
     * (e.g., © Photographer Name, from the IPTC Copyright Notice tag).
     */
    private String copyright;

    /**
     * Comma-separated IPTC keywords describing the image subject
     * (e.g., "Nature, Bird, Macro").
     */
    @Column(columnDefinition = "TEXT")
    private String keywords;

    /**
     * The IPTC Caption / Description field providing a narrative description of the image.
     */
    @Column(columnDefinition = "TEXT")
    private String description;
}
