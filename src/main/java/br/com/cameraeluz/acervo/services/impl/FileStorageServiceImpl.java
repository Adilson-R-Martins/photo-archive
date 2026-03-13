package br.com.cameraeluz.acervo.services.impl;

import br.com.cameraeluz.acervo.exceptions.FileStorageException;
import br.com.cameraeluz.acervo.services.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path baseStorageLocation;

    public FileStorageServiceImpl(@Value("${photoarchive.app.upload-dir}") String uploadDir) {
        this.baseStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create base storage directory.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String title) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        try {
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }

            // Usando UUID para evitar colisões
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Criar estrutura dinâmica: YYYY/MM/photos
            LocalDate now = LocalDate.now();
            String year = String.valueOf(now.getYear());
            String month = String.format("%02d", now.getMonthValue()); // Retorna '01', '10', etc.

            Path targetDir = this.baseStorageLocation.resolve(year).resolve(month).resolve("photos");
            Files.createDirectories(targetDir); // Garante que as pastas existam

            Path targetLocation = targetDir.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retorna o caminho relativo (ex: 2024/10/photos/uuid.jpg)
            return year + "/" + month + "/photos/" + uniqueFileName;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String relativePath) {
        try {
            // Previne falhas de segurança do tipo "Directory Traversal" (ex: ../../etc/passwd)
            if(relativePath.contains("..")) {
                throw new FileStorageException("Caminho de arquivo inválido: " + relativePath);
            }

            Path filePath = this.baseStorageLocation.resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + relativePath);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found: " + relativePath, ex);
        }
    }
}