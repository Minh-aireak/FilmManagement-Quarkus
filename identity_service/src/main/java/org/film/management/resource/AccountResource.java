package org.film.management.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.film.management.entity.Account;
import org.film.management.repository.AccountRepository;

import java.util.List;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    AccountRepository accountRepository;

    // 1. API lấy danh sách tất cả tài khoản
    // Yêu cầu: Bắt buộc phải có token mang quyền ADMIN
    @GET
    @RolesAllowed("ADMIN")
    public List<Account> getAllAccounts() {
        return accountRepository.listAll();
    }

    // 2. API lấy thông tin 1 tài khoản cụ thể dựa vào idAccount
    // Yêu cầu: Bắt buộc phải có token (có thể là ADMIN hoặc USER bình thường)
    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "USER"})
    public Account getAccount(@PathParam("id") String idAccount) {
        return accountRepository.findById(idAccount);
    }
}