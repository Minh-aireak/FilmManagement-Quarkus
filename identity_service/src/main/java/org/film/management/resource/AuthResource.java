package org.film.management.resource;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.film.management.entity.Account;
import org.film.management.repository.AccountRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AccountRepository accountRepository;

    @POST
    @Path("/login")
    public Response login(LoginRequest credentials) {
        Account account = accountRepository.findByEmail(credentials.getEmail());

        if (account == null || !BcryptUtil.matches(credentials.getPassword(), account.getPassword())) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Sai email hoặc mật khẩu").build();
        }

        Set<String> roles = new HashSet<>();
        if (account.getIdAccount().startsWith("AD_")) {
            roles.add("ADMIN");
        } else {
            roles.add("USER");
        }

        String token = Jwt.issuer("https://film-management.org/issuer")
                .upn(account.getEmail())
                .groups(roles)
                .claim("idAccount", account.getIdAccount())
                .claim("fullName", account.getFirstName() + " " + account.getLastName())
                .expiresIn(3600)
                .sign();

        return Response.ok(Map.of("token", token)).build();
    }

    @POST
    @Path("/register")
    @Transactional
    public Response register(Account newAccount) {
        if (accountRepository.findByEmail(newAccount.getEmail()) != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email đã tồn tại").build();
        }
        if (newAccount.getIdAccount() == null || newAccount.getIdAccount().trim().isEmpty()) {
            String randomId = "TK_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            newAccount.setIdAccount(randomId);
        }

        newAccount.setPassword(BcryptUtil.bcryptHash(newAccount.getPassword()));
        accountRepository.persist(newAccount);

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("message", "Đăng ký thành công"))
                .build();
    }

}