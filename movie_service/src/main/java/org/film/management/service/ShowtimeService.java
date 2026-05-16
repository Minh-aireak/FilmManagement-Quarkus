package org.film.management.service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.film.management.dto.request.AddShowtimeRequest;
import org.film.management.dto.response.PageResponse;
import org.film.management.entity.*;
import org.film.management.exception.AppException;
import org.film.management.exception.ErrorCode;
import org.film.management.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ShowtimeService {

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

    public PageResponse<Showtime> getShowtimesByMovie(String idMovie, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new AppException(ErrorCode.INVALID_PAGE);
        }

        movieRepository.findByIdOptional(idMovie)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        PanacheQuery<Showtime> query = showtimeRepository
                .find("idMovie = ?1 order by showTime desc", idMovie)
                .page(Page.of(page, size));

        return PageResponse.<Showtime>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(query.pageCount())
                .totalElement(query.count())
                .data(query.list())
                .build();
    }

    public PageResponse<Showtime> getAllShowtimes(int page, int size, String status) {
        if (page < 0 || size <= 0) {
            throw new AppException(ErrorCode.INVALID_PAGE);
        }

        PanacheQuery<Showtime> query;
        LocalDateTime now = LocalDateTime.now();

        if ("upcoming".equalsIgnoreCase(status)) {
            query = showtimeRepository.find("showTime > ?1", now);
        } else if ("past".equalsIgnoreCase(status)) {
            query = showtimeRepository.find("showTime <= ?1", now);
        } else {
            throw new AppException(ErrorCode.STATUS_SHOWTIME);
        }

        query.page(Page.of(page, size));

        return PageResponse.<Showtime>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(query.pageCount())
                .totalElement(query.count())
                .data(query.list())
                .build();
    }

    @Transactional
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
}