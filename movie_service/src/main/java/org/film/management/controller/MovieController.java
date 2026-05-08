package org.film.management.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.film.management.dto.request.MovieRequest;
import org.film.management.dto.response.ApiResponse;
import org.film.management.dto.response.PageResponse;
import org.film.management.entity.Movie;
import org.film.management.service.MovieService;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MovieController {

    @Inject
    MovieService movieService;

    @GET
    public ApiResponse<PageResponse<Movie>> getPageMovies(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {

        return ApiResponse.<PageResponse<Movie>>builder()
                .message("Get movies successfully!")
                .result(movieService.getPageMovies(page, size))
                .build();

    }

    @GET
    @Path("/{idMovie}")
    public ApiResponse<Movie> getMovieById(@PathParam("idMovie") String idMovie) {

        return ApiResponse.<Movie>builder()
                        .message("Get movie successfully!")
                        .result(movieService.getMovieById(idMovie))
                        .build();
    }

    @POST
    public ApiResponse<Movie> createMovie(@Valid MovieRequest movieRequest) {

        return ApiResponse.<Movie>builder()
                        .message("Create movie successfully!")
                        .result(movieService.createMovie(movieRequest))
                        .build();
    }

    @PUT
    @Path("/{idMovie}")
    public ApiResponse<Movie> updateMovie(@PathParam("idMovie") String idMovie,
                                          @Valid MovieRequest movieRequest) {

        return ApiResponse.<Movie>builder()
                        .message("Update movie successfully!")
                        .result(movieService.updateMovie(idMovie, movieRequest))
                        .build();
    }

    @DELETE
    @Path("/{idMovie}")
    public ApiResponse<Movie> deleteMovie(@PathParam("idMovie") String idMovie) {
        movieService.deleteMovie(idMovie);

        return ApiResponse.<Movie>builder()
                        .message("Delete movie successfully!")
                        .build();
    }
}