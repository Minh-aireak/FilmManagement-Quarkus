package org.film.management.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Room {
    @Id
    public String idRoom;
    public String nameRoom;
}