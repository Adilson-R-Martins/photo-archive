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
        // originalRelativePath é "2024/11/photos/uuid_foto.jpg"
        Path originalFile = this.baseStorageLocation.resolve(originalRelativePath);

        // Troca "photos" por "thumbnails" no caminho
        String thumbnailRelativePath = originalRelativePath.replace("photos", "thumbnails");
        Path thumbnailFile = this.baseStorageLocation.resolve(thumbnailRelativePath);

        try {
            // Cria a pasta de thumbnails do mês se não existir
            Files.createDirectories(thumbnailFile.getParent());

            Thumbnails.of(originalFile.toFile())
                    .size(1280, 1280)
                    .outputQuality(0.8)
                    .toFile(thumbnailFile.toFile());

            return thumbnailRelativePath.replace("\\", "/");
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao gerar thumbnail", ex);
        }
    }
}