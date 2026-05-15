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
    @Inject ShowtimeSeatRepo showtimeSeatRepo;
    @Inject BillRepo billRepo;
    @Inject TicketRepo ticketRepo;
    @Inject MovieRepo movieRepo;
    @Inject JsonWebToken jwt;
    @Inject ShowtimePriceRepo showtimePriceRepo;
    @Inject RoomSeatRepo roomSeatRepo;

    public List<Showtime> getShowtimesByMovie(String idMovie) {
        return showtimeRepo.find("idMovie", idMovie).list();
    }

    public List<String> getBookedSeats(String idShowtime) {
        List<ShowtimeSeat> bookedEntries = showtimeSeatRepo
                .find("idShowtime = ?1 AND status = 'BOOKED'", idShowtime)
                .list();

        return bookedEntries.stream()
                .map(s -> s.seatCode) // Sử dụng seatCode
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO bookTickets(BookingRequest request) {
        Showtime showtime = showtimeRepo.findById(request.idShowtime);
        if (showtime == null) throw new RuntimeException("Lịch chiếu không hợp lệ");

        ShowtimePrice priceConfig = showtimePriceRepo.findById(request.idShowtime);
        if (priceConfig == null) throw new RuntimeException("Chưa cấu hình giá cho lịch chiếu này");

        Bill bill = new Bill();
        bill.idBill = "Bill_" + java.util.UUID.randomUUID().toString().toUpperCase();

        if (jwt != null && jwt.containsClaim("idAccount")) {
            bill.idAccount = jwt.getClaim("idAccount");
        }

        bill.createdAt = java.time.LocalDateTime.now();
        bill.totalAmount = 0;

        billRepo.persist(bill);

        int totalAmount = 0;
        java.util.List<Ticket> tickets = new java.util.ArrayList<>();

        for (String seatCode : request.seatCodes) {
            ShowtimeSeat checkSeat = showtimeSeatRepo.findById(new ShowtimeSeatId(request.idShowtime, seatCode));
            if (checkSeat != null && "BOOKED".equals(checkSeat.status)) {
                throw new RuntimeException("Ghế " + seatCode + " đã có người đặt!");
            }

            RoomSeat roomSeat = roomSeatRepo.findById(new RoomSeatId(showtime.idRoom, seatCode));
            if (roomSeat == null) throw new RuntimeException("Mã ghế " + seatCode + " không tồn tại trong phòng chiếu");

            int ticketPrice = 0;
            if ("STANDARD".equals(roomSeat.typeSeat)) ticketPrice = priceConfig.standardPrice;
            else if ("VIP".equals(roomSeat.typeSeat)) ticketPrice = priceConfig.vipPrice;
            else if ("COUPLE".equals(roomSeat.typeSeat)) ticketPrice = priceConfig.couplePrice;

            totalAmount += ticketPrice;

            ShowtimeSeat newBookedSeat = new ShowtimeSeat();
            newBookedSeat.idShowtime = request.idShowtime;
            newBookedSeat.seatCode = seatCode;
            newBookedSeat.status = "BOOKED";
            showtimeSeatRepo.persist(newBookedSeat);

            Ticket ticket = new Ticket();
            ticket.idTicket = "T_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            ticket.idShowtime = request.idShowtime;
            ticket.seatCode = seatCode;
            ticket.price = ticketPrice;
            ticket.idBill = bill.idBill;

            ticketRepo.persist(ticket);
            tickets.add(ticket);
        }

        bill.totalAmount = totalAmount;

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
        if (bill == null) {
            throw new RuntimeException("Không tìm thấy hóa đơn có mã: " + idBill);
        }

        String currentAccountId = jwt.getClaim("idAccount");

        boolean isAdmin = jwt.getGroups() != null && jwt.getGroups().contains("ADMIN");

        if (!isAdmin && !bill.idAccount.equals(currentAccountId)) {
            throw new RuntimeException("Bạn không có quyền xem chi tiết hóa đơn của khách hàng khác!");
        }
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
        String currentAccountId = jwt.getClaim("idAccount");
        if (currentAccountId == null) {
            throw new RuntimeException("Không thể xác thực danh tính từ Token.");
        }

        Bill bill = billRepo.findById(idBill);
        if (bill == null) throw new RuntimeException("Hóa đơn không tồn tại");


        boolean isAdmin = jwt.getGroups() != null && jwt.getGroups().contains("ADMIN");

        if (!isAdmin && !currentAccountId.equals(bill.idAccount)) {
            throw new RuntimeException("Bạn không có quyền xóa hóa đơn của người khác!");
        }

        List<Ticket> tickets = ticketRepo.find("idBill", idBill).list();
        if (tickets.isEmpty()) throw new RuntimeException("Không tìm thấy vé trong hóa đơn");

        String idShowtime = tickets.get(0).idShowtime;
        Showtime showtime = showtimeRepo.findById(idShowtime);
        if (showtime != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cancellationDeadline = showtime.showTime.minusHours(1);
            if (now.isAfter(cancellationDeadline)) {
                throw new RuntimeException("Đã quá thời hạn hủy vé! Bạn chỉ có thể hủy tối thiểu 1 giờ trước giờ chiếu.");
            }
        }

        for (Ticket tk : tickets) {
            ticketRepo.delete(tk);

            ShowtimeSeatId seatId = new ShowtimeSeatId(tk.idShowtime, tk.seatCode);
            showtimeSeatRepo.deleteById(seatId);
        }

        billRepo.delete(bill);
    }

    public List<Bill> getAllBillsForAdmin() {
        return billRepo.find("order by createdAt desc").list();
    }

    @Transactional
    public Showtime addShowtime(AddShowtimeRequest request) {

        String idShowtime = "ST_" + UUID.randomUUID().toString().toUpperCase();

        Showtime showtime = new Showtime();
        showtime.idShowtime = idShowtime;
        showtime.idMovie = request.idMovie;
        showtime.idRoom = request.idRoom;
        showtime.showTime = request.showTime;
        showtimeRepo.persist(showtime);

        ShowtimePrice price = new ShowtimePrice();
        price.idShowtime = idShowtime;
        price.standardPrice = request.standardPrice;
        price.vipPrice = request.vipPrice;
        price.couplePrice = request.couplePrice;
        showtimePriceRepo.persist(price);

        List<RoomSeat> roomSeats = roomSeatRepo.find("idRoom", request.idRoom).list();

        for (RoomSeat rs : roomSeats) {
            ShowtimeSeat stSeat = new ShowtimeSeat();
            stSeat.idShowtime = idShowtime;
            stSeat.seatCode = rs.seatCode;
            stSeat.status = "AVAILABLE";
            showtimeSeatRepo.persist(stSeat);
        }

        return showtime;
    }
}