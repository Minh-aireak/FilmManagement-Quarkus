package org.film.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Price {
    @Id
    public String idPrice;
    public int standardPrice;
    public int vipPrice;
    public int triplePrice;
}