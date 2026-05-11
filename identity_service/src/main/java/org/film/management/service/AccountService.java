package org.film.management.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.film.management.entity.Account;
import org.film.management.repository.AccountRepository;
import org.film.management.controller.AuthResponse;
import org.film.management.controller.LoginRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AccountService {

    @Inject
    AccountRepository accountRepository;
    @Inject
    JsonWebToken jwt;

    public String getCurrentId() {
        return jwt.getClaim("idAccount"); // Tự động lấy ID của người đang gửi Request
    }

    // Logic Đăng nhập
    public AuthResponse authenticate(LoginRequest credentials) {
        Account account = accountRepository.findByEmail(credentials.getEmail());

        // Kiểm tra tài khoản và mật khẩu Bcrypt
        if (account == null || !BcryptUtil.matches(credentials.getPassword(), account.getPassword())) {
            return null; // Báo lỗi nếu sai
        }

        // --- PHÂN QUYỀN DỰA TRÊN CỘT ROLE ---
        Set<String> roles = new HashSet<>();
        // Lấy giá trị từ cột role (ADMIN hoặc CUSTOMER)
        String roleName = account.getRole().name();
        roles.add(roleName);

        String token = Jwt.issuer("https://film-management.org/issuer")
                .upn(account.getEmail())
                .groups(roles)
                .claim("idAccount", account.getIdAccount())
                .claim("fullName", account.getFirstName() + " " + account.getLastName())
                .expiresIn(3600)
                .sign();

        // Trả về cả Token và Role
        return new AuthResponse(token, roleName, account.getFirstName() + account.getLastName());
    }

    // Logic Đăng ký
    @Transactional
    public boolean register(Account newAccount) {
        // Kiểm tra trùng email
        if (accountRepository.findByEmail(newAccount.getEmail()) != null) {
            return false;
        }
        newAccount.setRole(org.film.management.entity.Role.CUSTOMER);
        // Tự động sinh ID nếu trống
        if (newAccount.getIdAccount() == null || newAccount.getIdAccount().trim().isEmpty()) {
            newAccount.setIdAccount("TK_" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Băm mật khẩu và lưu database
        newAccount.setPassword(BcryptUtil.bcryptHash(newAccount.getPassword()));
        accountRepository.persist(newAccount);
        return true;
    }

    // Logic lấy toàn bộ danh sách
    public List<Account> getAll() {
        return accountRepository.listAll();
    }

    // Logic lấy chi tiết theo ID
    public Account getById(String id) {
        return accountRepository.findById(id);
    }

    @Transactional
    public Account updateAccount(Account updatedData) {
        String id = getCurrentId(); // Lấy ID động, không cần truyền từ Resource sang
        if (id == null) return null;

        Account entity = accountRepository.findById(id);
        if (entity != null) {
            entity.setFirstName(updatedData.getFirstName());
            entity.setLastName(updatedData.getLastName());
            entity.setPhone(updatedData.getPhone());
            entity.setGender(updatedData.getGender());
            // Có thể cho phép sửa role nếu cần, hoặc chặn lại để bảo mật
            // entity.setRole(updatedData.getRole());
        }
        return entity;
    }

    @Transactional
    public boolean deleteAccount(String id) {
        return accountRepository.deleteById(id);
    }
}