package org.film.management.exception;

import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {

    MOVIE_NOT_FOUND(2001, "Movie not found", Response.Status.NOT_FOUND),
    INVALID_PAGE(2002, "Page must >= 0", Response.Status.BAD_REQUEST),
    CREATE_MOVIE_FAILED(2003, "Failed to create the movie!", Response.Status.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(9999, "Internal server error", Response.Status.INTERNAL_SERVER_ERROR);

    int code;
    String message;
    Response.Status statusCode;
}
