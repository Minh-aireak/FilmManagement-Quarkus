package org.film.management.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.film.management.exception.AppException;
import org.film.management.exception.ErrorCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@ApplicationScoped
public class ImageService {

    private static final String UPLOAD_DIR = "uploads/";

    public String upload(java.io.File file, String fileName) {
        String newName = UUID.randomUUID().toString().substring(0, 8) + "_" + fileName;

        File dest = new File(UPLOAD_DIR + newName);

        try {
            Files.copy(file.toPath(), dest.toPath());
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_ERROR);
        }

        return "http://localhost:8081/images/" + newName;
    }
}