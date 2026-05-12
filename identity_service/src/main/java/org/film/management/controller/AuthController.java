package org.film.management.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.film.management.entity.Account;
import org.film.management.service.AccountService;

import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    AccountService accountService;

    @POST
    @Path("/login")
    public Response login(LoginRequest credentials) {
        AuthResponse authResponse = accountService.authenticate(credentials);

        if (authResponse == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Sai email hoặc mật khẩu"))
                    .build();
        }

        return Response.ok(authResponse).build();
    }

    @POST
    @Path("/register")
    public Response register(Account newAccount) {
        // Gọi Service xử lý logic
        boolean success = accountService.register(newAccount);

        if (!success) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Email đã tồn tại"))
                    .build();
        }

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("message", "Đăng ký thành công"))
                .build();
    }
}