package org.film.management.service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.film.management.dto.request.AddShowtimeRequest;
import org.film.management.dto.request.MovieRequest;
import org.film.management.dto.response.PageResponse;
import org.film.management.entity.*;
import org.film.management.exception.AppException;
import org.film.management.exception.ErrorCode;
import org.film.management.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MovieService {

    @Inject
    MovieRepository movieRepository;

    @Inject
    ShowtimeRepository showtimeRepository;

    @Inject
    ShowtimePriceRepository showtimePriceRepository;

    @Inject
    RoomSeatRepository roomSeatRepository;

    @Inject
    ShowtimeSeatRepository showtimeSeatRepository;

    @Inject
    TicketRepository ticketRepository;

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

    public List<Showtime> getShowtimesByMovie(String idMovie) {
        movieRepository.findByIdOptional(idMovie)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        return showtimeRepository.find("idMovie", idMovie).list();
    }

    public PageResponse<Movie> getMoviesByDate(String dateStr, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new AppException(ErrorCode.INVALID_PAGE);
        }

        LocalDate parsedDate = LocalDate.parse(dateStr);

        LocalDateTime startOfDay = parsedDate.atStartOfDay();
        LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59);

        PanacheQuery<Movie> query = movieRepository.find(
                "idMovie IN (SELECT s.idMovie FROM Showtime s WHERE s.showTime >= ?1 AND s.showTime <= ?2)",
                startOfDay, endOfDay
        ).page(Page.of(page, size));

        return PageResponse.<Movie>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(query.pageCount())
                .totalElement(query.count())
                .data(query.list())
                .build();
    }

    public PageResponse<Showtime> getAllShowtimes(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new AppException(ErrorCode.INVALID_PAGE);
        }

        PanacheQuery<Showtime> query = showtimeRepository.findAll()
                .page(Page.of(page, size));

        return PageResponse.<Showtime>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(query.pageCount())
                .totalElement(query.count())
                .data(query.list())
                .build();
    }

    public Showtime createShowtime(AddShowtimeRequest request) {
        String idShowtime = "ST_" + UUID.randomUUID().toString().toUpperCase();

        Showtime showtime = Showtime.builder()
                .idShowtime(idShowtime)
                .idMovie(request.idMovie)
                .idRoom(request.idRoom)
                .showTime(request.showTime)
                .build();

        showtimeRepository.persist(showtime);

        ShowtimePrice price = ShowtimePrice.builder()
                .idShowtime(idShowtime)
                .standardPrice(request.standardPrice)
                .vipPrice(request.vipPrice)
                .couplePrice(request.couplePrice)
                .build();

        showtimePriceRepository.persist(price);

        List<RoomSeat> roomSeats = roomSeatRepository.find("idRoom", request.idRoom).list();

        for (RoomSeat rs : roomSeats) {
            ShowtimeSeat stSeat = ShowtimeSeat.builder()
                    .idShowtime(idShowtime)
                    .seatCode(rs.seatCode)
                    .status("AVAILABLE")
                    .build();
            showtimeSeatRepository.persist(stSeat);
        }

        return showtime;
    }

    @Transactional
    public void deleteShowtime(String idShowtime) {
        Showtime showtime = showtimeRepository.findById(idShowtime);
        if (showtime == null) throw new RuntimeException("Lịch chiếu không tồn tại");
        long ticketCount = ticketRepository.find("idShowtime", idShowtime).count();
        if (ticketCount > 0) {
            throw new AppException(ErrorCode.DELETE_SHOWTIME);
        }
        showtimeSeatRepository.delete("idShowtime", idShowtime);
        showtimePriceRepository.deleteById(idShowtime);
        showtimeRepository.delete(showtime);
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