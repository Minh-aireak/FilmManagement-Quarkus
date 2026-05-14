package org.film.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Showtime {
    @Id
    public String idShowtime;
    public LocalDateTime showTime;
    public String idMovie;
    public String idRoom;
}