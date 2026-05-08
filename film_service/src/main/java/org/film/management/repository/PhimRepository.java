package org.film.management.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.film.management.entity.Phim;

@ApplicationScoped
public class PhimRepository implements PanacheRepository<Phim> {

}
