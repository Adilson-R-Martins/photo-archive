package br.com.cameraeluz.acervo.dto;

import lombok.Data;

/**
 * Read-only projection of {@link br.com.cameraeluz.acervo.models.ExifData} for API responses.
 *
 * <p>Decouples the public API shape from the JPA entity so that column renames,
 * new persistence annotations, or embedded-object refactorings do not leak into
 * the response contract.</p>
 *
 * <p>The IPTC title tag is surfaced as {@code iptcTitle} (mapped from the
 * {@code exif_title} column) to avoid ambiguity with the photo's own display
 * title in {@link PhotoResponseDTO}.</p>
 */
@Data
public class ExifDataDTO {

    /** IPTC title tag embedded in the image file (distinct from the photo's display title). */
    private String iptcTitle;

    private String cameraModel;
    private String lens;
    private String focalLength;
    private String aperture;
    private String shutterSpeed;
    private String iso;
    private String captureDate;

    /** Software used to process or edit the image (e.g., Adobe Lightroom, Photoshop). */
    private String software;

    /** Copyright notice embedded in the image (e.g., © Photographer Name). */
    private String copyright;

    /** Comma-separated IPTC keywords describing the image subject. */
    private String keywords;

    /** IPTC Caption / Description field providing a narrative description of the image. */
    private String description;
}
