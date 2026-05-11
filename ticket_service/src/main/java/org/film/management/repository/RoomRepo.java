package org.film.management.repository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.film.management.entity.Room;

@ApplicationScoped
public class RoomRepo implements PanacheRepositoryBase<Room, String> {}