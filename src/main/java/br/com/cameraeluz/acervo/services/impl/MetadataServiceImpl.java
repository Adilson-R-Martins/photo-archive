package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.models.ExifData;
import br.com.cameraeluz.acervo.services.MetadataService;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
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

            // Extract Camera Model
            ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Dir != null) {
                exifData.setCameraModel(ifd0Dir.getString(ExifIFD0Directory.TAG_MODEL));
            }

            // Extract Technical Specs
            ExifSubIFDDirectory subIfdDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIfdDir != null) {
                exifData.setLens(subIfdDir.getString(ExifSubIFDDirectory.TAG_LENS_MODEL));
                exifData.setAperture(subIfdDir.getString(ExifSubIFDDirectory.TAG_FNUMBER));
                exifData.setIso(subIfdDir.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                exifData.setShutterSpeed(subIfdDir.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                exifData.setFocalLength(subIfdDir.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                exifData.setCaptureDate(subIfdDir.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
            }
        } catch (Exception e) {
            // Log the error and return empty exifData to avoid crashing the upload
            System.err.println("Error extracting metadata: " + e.getMessage());
        }

        return exifData;
    }
}