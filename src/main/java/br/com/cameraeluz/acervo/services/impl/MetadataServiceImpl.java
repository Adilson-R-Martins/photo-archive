package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.models.ExifData;
import br.com.cameraeluz.acervo.services.MetadataService;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Implementation of MetadataService using the 'metadata-extractor' library.
 * Maps standard EXIF tags to the application's ExifData model.
 */
@Service
public class MetadataServiceImpl implements MetadataService {

    @Override
    public ExifData extractMetadata(MultipartFile file) {
        ExifData exifData = new ExifData();

        try (InputStream is = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);

            // --- 1. EXIF IFD0 (Onde geralmente moram o Modelo e o Software) ---
            ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Dir != null) {
                // Tentativa 1 para Modelo
                exifData.setCameraModel(ifd0Dir.getString(ExifIFD0Directory.TAG_MODEL));
                // Tentativa para Software
                exifData.setSoftware(ifd0Dir.getString(ExifIFD0Directory.TAG_SOFTWARE));
            }

            // --- 2. EXIF SubIFD (Dados técnicos e Fallback de Modelo) ---
            ExifSubIFDDirectory subIfdDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIfdDir != null) {
                // Se o modelo ainda for nulo, tenta pegar do SubIFD
                if (exifData.getCameraModel() == null) {
                    exifData.setCameraModel(subIfdDir.getString(ExifSubIFDDirectory.TAG_MODEL));
                }
                exifData.setLens(subIfdDir.getString(ExifSubIFDDirectory.TAG_LENS_MODEL));
                exifData.setAperture(subIfdDir.getString(ExifSubIFDDirectory.TAG_FNUMBER));
                exifData.setShutterSpeed(subIfdDir.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                exifData.setIso(subIfdDir.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                exifData.setFocalLength(subIfdDir.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                exifData.setCaptureDate(subIfdDir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
            }

            // --- 3. IPTC (Copyright, Keywords e Description) ---
            IptcDirectory iptcDir = metadata.getFirstDirectoryOfType(IptcDirectory.class);
            if (iptcDir != null) {
                exifData.setCopyright(iptcDir.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE));
                exifData.setDescription(iptcDir.getString(IptcDirectory.TAG_CAPTION)); // This is the "Description"

                String[] keywords = iptcDir.getStringArray(IptcDirectory.TAG_KEYWORDS);
                if (keywords != null) {
                    exifData.setKeywords(String.join(", ", keywords));
                }
            }
        } catch (Exception e) {
            // Log the error and return empty exifData to avoid crashing the upload
            System.err.println("Error extracting metadata: " + e.getMessage());
        }

        return exifData;
    }
}