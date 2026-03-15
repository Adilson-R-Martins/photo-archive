package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.models.ExifData;
import br.com.cameraeluz.acervo.services.MetadataService;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Implementation of MetadataService using the 'metadata-extractor' library.
 * Maps standard EXIF tags to the application's ExifData model.
 */
@Service
public class MetadataServiceImpl implements MetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataServiceImpl.class);

    @Override
    public ExifData extractMetadata(MultipartFile file) {
        ExifData exifData = new ExifData();

        try (InputStream is = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);

            ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Dir != null) {
                exifData.setCameraModel(ifd0Dir.getString(ExifIFD0Directory.TAG_MODEL));
                exifData.setSoftware(ifd0Dir.getString(ExifIFD0Directory.TAG_SOFTWARE));
            }

            ExifSubIFDDirectory subIfdDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (subIfdDir != null) {
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

            IptcDirectory iptcDir = metadata.getFirstDirectoryOfType(IptcDirectory.class);
            if (iptcDir != null) {
                exifData.setTitle(iptcDir.getString(IptcDirectory.TAG_OBJECT_NAME));
                exifData.setDescription(iptcDir.getString(IptcDirectory.TAG_CAPTION));
                exifData.setCopyright(iptcDir.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE));

                String[] keywords = iptcDir.getStringArray(IptcDirectory.TAG_KEYWORDS);
                if (keywords != null) {
                    exifData.setKeywords(String.join(", ", keywords));
                }
            }
        } catch (Exception e) {
            logger.warn("Metadata extraction failed for '{}': {}. The file will be stored without EXIF/IPTC data.",
                    file.getOriginalFilename(), e.getMessage());
        }

        return exifData;
    }
}