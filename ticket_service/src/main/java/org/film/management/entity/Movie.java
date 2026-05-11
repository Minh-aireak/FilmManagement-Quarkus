package org.film.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movie")
public class Movie {
    @Id
    public String idMovie;

    public String nameMovie;
    public String author;
    public String actors;
    public int duration;
    public String language;
    public String description;
    public String image;
}