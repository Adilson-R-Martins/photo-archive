package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.services.ImageService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageServiceImpl implements ImageService {

    private final Path baseStorageLocation;

    public ImageServiceImpl(@Value("${photoarchive.app.upload-dir}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String generateWebOptimizedVersion(String originalRelativePath) {
        Path originalFile = this.baseStorageLocation.resolve(originalRelativePath);

        // Substitui apenas o segmento de diretório "photos" por "thumbnails",
        // sem risco de corromper títulos que contenham a palavra "photos"
        Path relativePath = Paths.get(originalRelativePath);
        Path parent = relativePath.getParent();           // ex: "2024/11/photos"
        Path grandParent = parent.getParent();            // ex: "2024/11"
        Path thumbParent = grandParent.resolve("thumbnails"); // ex: "2024/11/thumbnails"
        Path thumbnailRelativePath = thumbParent.resolve(relativePath.getFileName()); // ex: "2024/11/thumbnails/uuid_foto.jpg"

        Path thumbnailFile = this.baseStorageLocation.resolve(thumbnailRelativePath);

        try {
            Files.createDirectories(thumbnailFile.getParent());

            Thumbnails.of(originalFile.toFile())
                    .size(1280, 1280)
                    .outputQuality(0.8)
                    .toFile(thumbnailFile.toFile());

            return thumbnailRelativePath.toString().replace("\\", "/");
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao gerar thumbnail: " + ex.getMessage(), ex);
        }
    }
}