package org.film.management.service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.film.management.dto.request.MovieRequest;
import org.film.management.dto.response.PageResponse;
import org.film.management.entity.Category;
import org.film.management.entity.Movie;
import org.film.management.exception.AppException;
import org.film.management.exception.ErrorCode;
import org.film.management.repository.MovieRepository;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class MovieService {

    @Inject
    MovieRepository movieRepository;

    public PageResponse<Movie> getPageMovies(int page, int size) {

        if (page < 0 || size <= 0) {
            throw new AppException(ErrorCode.INVALID_PAGE);
        }

        PanacheQuery<Movie> query = movieRepository.findAll(Sort.descending("createdAt"))
                        .page(Page.of(page, size));

        return PageResponse.<Movie>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(query.pageCount())
                .totalElement(query.count())
                .data(query.list())
                .build();
    }

    public Movie getMovieById(String idMovie) {
        return movieRepository.findByIdOptional(idMovie)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
    }

    @Transactional
    public Movie createMovie(MovieRequest movieRequest) {
        try {

            Movie movie = Movie.builder()
                    .nameMovie(movieRequest.getNameMovie())
                    .author(movieRequest.getAuthor())
                    .actors(movieRequest.getActors())
                    .duration(movieRequest.getDuration())
                    .language(movieRequest.getLanguage())
                    .description(movieRequest.getDescription())
                    .image(movieRequest.getImage())
                    .createdAt(LocalDateTime.now())
                    .categories(movieRequest.getIdCategories())
                    .build();

            movieRepository.persist(movie);

            return movie;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.CREATE_MOVIE_FAILED);
        }
    }

    @Transactional
    public Movie updateMovie(String idMovie, MovieRequest movieRequest) {

        Movie oldMovie = movieRepository.findByIdOptional(idMovie)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        oldMovie.setNameMovie(movieRequest.getNameMovie());
        oldMovie.setAuthor(movieRequest.getAuthor());
        oldMovie.setActors(movieRequest.getActors());
        oldMovie.setDuration(movieRequest.getDuration());
        oldMovie.setLanguage(movieRequest.getLanguage());
        oldMovie.setDescription(movieRequest.getDescription());
        oldMovie.setImage(movieRequest.getImage());
        oldMovie.setCategories(movieRequest.getIdCategories());

        return oldMovie;
    }

    @Transactional
    public void deleteMovie(String idMovie) {

        Movie movie = movieRepository.findByIdOptional(idMovie)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        movieRepository.delete(movie);
    }
}