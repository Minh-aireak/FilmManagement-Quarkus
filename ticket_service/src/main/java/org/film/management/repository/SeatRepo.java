package org.film.management.repository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.film.management.entity.Seat;

@ApplicationScoped
public class SeatRepo implements PanacheRepositoryBase<Seat, String> {}