package org.film.management.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.film.management.entity.Account;
import org.film.management.service.AccountService;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountController {

    @Inject
    AccountService accountService;

    @GET
    @RolesAllowed("ADMIN")
    public List<Account> getAllAccounts() {
        return accountService.getAll();
    }

    @GET
    @Path("/{idAccount}")
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public Account getAccount(@PathParam("idAccount") String idAccount) {
        return accountService.getById(idAccount);
    }

    @PUT
    @Path("/update-profile")
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public Response updateProfile(Account account) {
        Account result = accountService.updateAccount(account);
        return result != null ? Response.ok(result).build() : Response.status(401).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public void delete(@PathParam("id") String id) {
        boolean deleted = accountService.deleteAccount(id);
        if (!deleted) {
            throw new NotFoundException("Không tìm thấy tài khoản để xóa");
        }
    }

}