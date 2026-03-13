package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.exceptions.FileStorageException;
import br.com.cameraeluz.acervo.services.ImageService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageServiceImpl implements ImageService {

    private final Path baseStorageLocation;

    // Agora usamos apenas o upload-dir base, as pastas são relativas a ele.
    public ImageServiceImpl(@Value("${photoarchive.app.upload-dir}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String generateWebOptimizedVersion(String originalRelativePath) {
        try {
            // originalRelativePath chega como: "2024/10/photos/uuid.jpg"
            String[] parts = originalRelativePath.split("/");
            if (parts.length < 4) {
                throw new IllegalArgumentException("Formato de caminho de arquivo inválido.");
            }

            String year = parts[0];
            String month = parts[1];
            String fileName = parts[parts.length - 1]; // pega só o "uuid.jpg"

            // Caminho absoluto do arquivo original
            File originalFile = this.baseStorageLocation.resolve(originalRelativePath).toFile();
            if (!originalFile.exists()) {
                throw new FileStorageException("Arquivo original não encontrado: " + originalRelativePath);
            }

            // Define e cria a pasta para as thumbnails: YYYY/MM/thumbnails
            Path thumbnailsDir = this.baseStorageLocation.resolve(year).resolve(month).resolve("thumbnails");
            Files.createDirectories(thumbnailsDir);

            // Nome e arquivo final otimizado
            String optimizedFileName = "web_" + fileName;
            File optimizedFile = thumbnailsDir.resolve(optimizedFileName).toFile();

            // Processamento da imagem
            Thumbnails.of(originalFile)
                    .size(1280, 1280)
                    .outputQuality(0.8)
                    .toFile(optimizedFile);

            // Retorna o caminho relativo da miniatura
            return year + "/" + month + "/thumbnails/" + optimizedFileName;

        } catch (Exception ex) {
            throw new RuntimeException("Falha ao gerar a imagem web otimizada para: " + originalRelativePath, ex);
        }
    }
}