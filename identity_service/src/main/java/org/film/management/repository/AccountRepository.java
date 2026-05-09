package org.film.management.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.film.management.entity.Account;

@ApplicationScoped
// Dùng PanacheRepositoryBase vì Khóa chính (idAccount) của bạn là kiểu String
public class AccountRepository implements PanacheRepositoryBase<Account, String> {

    public Account findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public Account findByPhone(String phone) {
        return find("phone", phone).firstResult();
    }
}