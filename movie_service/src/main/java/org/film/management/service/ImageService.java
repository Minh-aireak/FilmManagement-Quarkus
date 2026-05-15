package org.film.management.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.film.management.exception.AppException;
import org.film.management.exception.ErrorCode;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@ApplicationScoped
public class ImageService {

    private static final String UPLOAD_DIR = "uploads/";

    public String upload(FileUpload file) {

        File dir = new File(UPLOAD_DIR);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        String extension = "";

        if (file.fileName().contains(".")) {
            extension = file.fileName()
                    .substring(file.fileName().lastIndexOf("."));
        }

        String newName = UUID.randomUUID()
                .toString()
                .substring(0, 8) + extension;

        File dest = new File(UPLOAD_DIR + newName);

        try {
            Files.copy(
                    file.uploadedFile(),
                    dest.toPath()
            );

        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_ERROR);
        }

        return "http://localhost:8081/images/" + newName;
    }
}