package org.film.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class Seat {
    @Id
    public String idSeat;

    @Enumerated(EnumType.STRING)
    public TypeSeat typeSeat;

    public enum TypeSeat {
        STANDARD, VIP, TRIPLE
    }
}