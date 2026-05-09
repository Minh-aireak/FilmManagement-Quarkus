package org.film.management.resource;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
