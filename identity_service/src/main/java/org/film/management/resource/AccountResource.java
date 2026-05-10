package org.film.management.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.film.management.entity.Account;
import org.film.management.repository.AccountRepository;
import org.film.management.service.AccountService;

import java.util.List;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    AccountRepository accountRepository;
    AccountService accountService;

    @GET
    @RolesAllowed("ADMIN")
    public List<Account> getAllAccounts() {
        return accountRepository.listAll();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public Account getAccount(@PathParam("id") String idAccount) {
        return accountRepository.findById(idAccount);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CUSTOMER"}) // Cả 2 đều có quyền sửa thông tin cá nhân
    public Account update(@PathParam("id") String id, Account account) {
        return accountService.updateAccount(id, account);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN") // Chỉ Admin mới có quyền xóa tài khoản
    public void delete(@PathParam("id") String id) {
        boolean deleted = accountService.deleteAccount(id);
        if (!deleted) {
            throw new NotFoundException("Không tìm thấy tài khoản để xóa");
        }
    }
}