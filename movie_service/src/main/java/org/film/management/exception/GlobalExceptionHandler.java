package org.film.management.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.film.management.dto.response.ApiResponse;

@Provider
public class GlobalExceptionHandler
        implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof AppException appException) {

            ErrorCode errorCode =
                    appException.getErrorCode();

            ApiResponse<Object> response =
                    ApiResponse.builder()
                            .success(false)
                            .code(errorCode.getCode())
                            .message(errorCode.getMessage())
                            .build();

            return Response
                    .status(errorCode.getStatusCode())
                    .entity(response)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (exception instanceof ConstraintViolationException ex) {

            String error = ex.getConstraintViolations()
                    .stream()
                    .findFirst()
                    .map(ConstraintViolation::getMessage)
                    .orElse("Validation failed");

            ApiResponse<Object> response =
                    ApiResponse.builder()
                            .success(false)
                            .code(400)
                            .message(error)
                            .build();

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(response)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        ApiResponse<Object> response =
                ApiResponse.builder()
                        .success(false)
                        .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                        .message(exception.getMessage())
                        .build();

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}