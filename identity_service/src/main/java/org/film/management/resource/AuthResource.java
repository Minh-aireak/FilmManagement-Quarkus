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
import java.util.Set;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AccountRepository accountRepository;

    // 1. HÀM ĐĂNG NHẬP (Đã cập nhật kiểm tra Bcrypt)
    @POST
    @Path("/login")
    public Response login(LoginRequest credentials) {
        Account account = accountRepository.findByEmail(credentials.getEmail());

        // SỬ DỤNG BcryptUtil.matches() ĐỂ SO SÁNH MẬT KHẨU
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

        return Response.ok("{\"token\":\"" + token + "\"}").build();
    }

    // 2. HÀM ĐĂNG KÝ (Tự động băm mật khẩu khi tạo user mới)
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
        // BĂM MẬT KHẨU TRƯỚC KHI LƯU VÀO DATABASE
        newAccount.setPassword(BcryptUtil.bcryptHash(newAccount.getPassword()));
        accountRepository.persist(newAccount);

        return Response.status(Response.Status.CREATED).entity("Đăng ký thành công").build();
    }

    // 3. API CHỮA CHÁY (Chạy 1 lần duy nhất để băm pass của admin & user cũ)
//    @GET
//    @Path("/hash-old-passwords")
//    @Transactional
//    public Response hashOldPasswords() {
//        List<Account> accounts = accountRepository.listAll();
//        int count = 0;
//        for (Account acc : accounts) {
//            // Chuỗi Bcrypt luôn bắt đầu bằng "$2a$" hoặc "$2b$". Nếu không phải thì mã hóa nó!
//            if (!acc.getPassword().startsWith("$2")) {
//                acc.setPassword(BcryptUtil.bcryptHash(acc.getPassword()));
//                count++;
//            }
//        }
//        return Response.ok("Đã mã hóa thành công " + count + " tài khoản cũ trong Database!").build();
//    }
}