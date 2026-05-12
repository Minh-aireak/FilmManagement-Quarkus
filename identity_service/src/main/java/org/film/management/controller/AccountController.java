package org.film.management.controller;

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
public class AccountController {

    @Inject
    AccountRepository accountRepository;

    @Inject
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
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public Account update(@PathParam("id") String id, Account account) {
        return accountService.updateAccount(id, account);
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