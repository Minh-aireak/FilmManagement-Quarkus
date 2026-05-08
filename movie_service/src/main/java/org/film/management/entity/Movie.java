package org.film.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movie")
public class Movie {
    @Id
    private String idMovie;
    private String nameMovie;
    private String author;
    private String actors;
    private String duration;
    private String language;
    private String description;
    private String image;

    @PrePersist
    public void generateId() {

        if (this.idMovie == null || this.idMovie.isEmpty()) {
            this.idMovie =
                    "Movie_" + UUID.randomUUID();
        }
    }
}