package org.film.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Bill {
    @Id
    public String idBill;
    public String idAccount;
    public LocalDateTime bookingTime;
    public int totalAmount;
}
