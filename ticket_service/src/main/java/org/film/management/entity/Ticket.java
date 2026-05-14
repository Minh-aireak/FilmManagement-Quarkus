package org.film.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Ticket {
    @Id
    public String idTicket;
    public String idBill;
    public String idShowtime;
    public String seatCode;
    public int price;
}