package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.exceptions.FileStorageException;
import br.com.cameraeluz.acervo.services.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path baseStorageLocation;

    public FileStorageServiceImpl(@Value("${photoarchive.app.upload-dir}") String uploadDir) {
        // Agora aponta para "uploads"
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String storeFile(MultipartFile file, String title) {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        Path relativeFolder = Paths.get(year, month, "photos");
        Path fullPath = this.baseStorageLocation.resolve(relativeFolder);

        try {
            Files.createDirectories(fullPath);

            // Sanitiza o nome original para remover path traversal e caracteres perigosos
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "file";
            String safeName = Paths.get(originalName).getFileName().toString()
                    .replaceAll("[^a-zA-Z0-9._-]", "_");

            String fileName = UUID.randomUUID() + "_" + safeName;
            Files.copy(file.getInputStream(), fullPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            return relativeFolder.resolve(fileName).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new FileStorageException("Erro ao salvar arquivo", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String relativePath) {
        try {
            Path filePath = this.baseStorageLocation.resolve(relativePath).normalize();

            if (!filePath.startsWith(this.baseStorageLocation)) {
                throw new FileStorageException("Acesso negado: caminho fora do diretório permitido.");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Arquivo não encontrado ou ilegível: " + relativePath);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Erro ao localizar arquivo: " + relativePath, ex);
        }
    }
}