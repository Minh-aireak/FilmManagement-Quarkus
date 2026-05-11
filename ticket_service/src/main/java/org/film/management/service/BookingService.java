package org.film.management.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.film.management.dto.*;
import org.film.management.entity.*;
import org.film.management.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @Inject ShowtimeRepo showtimeRepo;
    @Inject SeatRepo seatRepo;
    @Inject ShowtimeSeatRepo showtimeSeatRepo;
    @Inject PriceRepo priceRepo;
    @Inject BillRepo billRepo;
    @Inject TicketRepo ticketRepo;
    @Inject MovieRepo movieRepo;
    @Inject JsonWebToken jwt;

    public List<Showtime> getShowtimesByMovie(String idMovie) {
        return showtimeRepo.find("idMovie", idMovie).list();
    }

    public List<SeatStatusDTO> getSeatStatus(String idShowtime) {
        List<Seat> allSeats = seatRepo.listAll();
        List<ShowtimeSeat> bookedSeats = showtimeSeatRepo.find("idShowtime", idShowtime).list();
        List<String> bookedSeatIds = bookedSeats.stream().map(bs -> bs.idSeat).collect(Collectors.toList());

        List<SeatStatusDTO> seatStatuses = new ArrayList<>();
        for (Seat seat : allSeats) {
            String status = bookedSeatIds.contains(seat.idSeat) ? "BOOKED" : "AVAILABLE";
            seatStatuses.add(new SeatStatusDTO(seat.idSeat, seat.typeSeat.toString(), status));
        }
        return seatStatuses;
    }

    @Transactional
    public BookingResponseDTO bookTickets(BookingRequest request) {
        String currentAccountId = jwt.getClaim("idAccount");
        if (currentAccountId == null) {
            throw new RuntimeException("Không thể xác thực danh tính. Vui lòng đăng nhập lại!");
        }

        Showtime showtime = showtimeRepo.findById(request.idShowtime);
        if (showtime == null) throw new RuntimeException("Lịch chiếu không hợp lệ");

        Price priceConfig = priceRepo.findById(showtime.idPrice);
        int totalAmount = 0;

        for (String seatId : request.seatIds) {
            ShowtimeSeat checkSeat = showtimeSeatRepo.findById(new ShowtimeSeatId(request.idShowtime, seatId));
            if (checkSeat != null && "BOOKED".equals(checkSeat.status)) {
                throw new RuntimeException("Ghế " + seatId + " đã có người đặt!");
            }
            Seat seat = seatRepo.findById(seatId);
            if (seat.typeSeat.name().equals("STANDARD")) totalAmount += priceConfig.standardPrice;
            else if (seat.typeSeat.name().equals("VIP")) totalAmount += priceConfig.vipPrice;
            else if (seat.typeSeat.name().equals("TRIPLE")) totalAmount += priceConfig.triplePrice;

            ShowtimeSeat newBookedSeat = new ShowtimeSeat();
            newBookedSeat.idShowtime = request.idShowtime;
            newBookedSeat.idSeat = seatId;
            newBookedSeat.status = "BOOKED";
            showtimeSeatRepo.persist(newBookedSeat);
        }

        Bill bill = new Bill();
        bill.idBill = "B" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        bill.idAccount = currentAccountId;
        bill.bookingTime = LocalDateTime.now();
        bill.totalAmount = totalAmount;
        billRepo.persist(bill);

        List<Ticket> tickets = new ArrayList<>();
        for (String seatId : request.seatIds) {
            Ticket ticket = new Ticket();
            ticket.idTicket = "T_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            ticket.idShowtime = request.idShowtime;
            ticket.idSeat = seatId;
            ticket.idPrice = showtime.idPrice;
            ticket.idBill = bill.idBill;

            ticketRepo.persist(ticket);

            tickets.add(ticket);
        }

        showtime.availableSeats -= request.seatIds.size();
        return new BookingResponseDTO(bill, tickets);
    }

    public List<Movie> getMoviesByDate(String dateStr) {
        LocalDate parsedDate = LocalDate.parse(dateStr);

        LocalDateTime startOfDay = parsedDate.atStartOfDay();
        LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59);

        List<Showtime> showtimes = showtimeRepo.find("showTime >= ?1 AND showTime <= ?2", startOfDay, endOfDay).list();

        List<String> movieIds = showtimes.stream().map(s -> s.idMovie).distinct().collect(Collectors.toList());
        List<Movie> movies = new ArrayList<>();
        for (String id : movieIds) {
            Movie m = movieRepo.findById(id);
            if (m != null) movies.add(m);
        }
        return movies;
    }

    public List<Showtime> getAllShowtimes() {
        return showtimeRepo.listAll();
    }

    public BookingResponseDTO getBillDetail(String idBill) {
        Bill bill = billRepo.findById(idBill);
        if (bill == null) throw new RuntimeException("Không tìm thấy hóa đơn");

        List<Ticket> tickets = ticketRepo.find("idBill", idBill).list();
        return new BookingResponseDTO(bill, tickets);
    }

    public List<Bill> getMyBookingHistory() {
        String currentAccountId = jwt.getClaim("idAccount");
        if (currentAccountId == null) {
            throw new RuntimeException("Không thể xác thực danh tính.");
        }
        return billRepo.find("idAccount", currentAccountId).list();
    }

    @Transactional
    public void cancelBill(String idBill) {
        Bill bill = billRepo.findById(idBill);
        if (bill == null) throw new RuntimeException("Hóa đơn không tồn tại");

        List<Ticket> tickets = ticketRepo.find("idBill", idBill).list();
        if (tickets.isEmpty()) throw new RuntimeException("Không tìm thấy vé trong hóa đơn");

        String idShowtime = tickets.get(0).idShowtime;
        Showtime showtime = showtimeRepo.findById(idShowtime);

        for (Ticket tk : tickets) {
            ShowtimeSeatId seatId = new ShowtimeSeatId(tk.idShowtime, tk.idSeat);
            showtimeSeatRepo.deleteById(seatId);

            ticketRepo.delete(tk);
        }

        if (showtime != null) {
            showtime.availableSeats += tickets.size();
        }

        billRepo.delete(bill);
    }

    public List<Bill> getAllBillsForAdmin() {
        return billRepo.find("order by bookingTime desc").list();
    }
}