package org.film.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Ticket {
    @Id
    public String idTicket;
    public String idShowtime;
    public String idSeat;
    public String idPrice;
    public String idBill;
}