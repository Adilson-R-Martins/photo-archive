package br.com.cameraeluz.acervo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PhotoArchiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotoArchiveApplication.class, args);
    }

}
