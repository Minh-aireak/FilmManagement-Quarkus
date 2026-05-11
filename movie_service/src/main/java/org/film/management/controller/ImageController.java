package org.film.management.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.film.management.dto.response.ApiResponse;
import org.film.management.service.ImageService;

@Path("/images")
public class ImageController {

    @Inject
    ImageService imageService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public ApiResponse<String> upload(@FormParam("file") java.io.File file,
                              @FormParam("fileName") String fileName) throws Exception {

        return ApiResponse.<String>builder()
                .result(imageService.upload(file, fileName))
                .build();
    }
}
