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
        // Lấy idAccount trực tiếp từ Token đang gửi lên
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
            // Tính tiền...
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
        // GÁN idAccount TỪ TOKEN VÀO HÓA ĐƠN
        bill.idAccount = currentAccountId;
        bill.bookingTime = LocalDateTime.now();
        bill.totalAmount = totalAmount;
        billRepo.persist(bill);

        List<Ticket> tickets = new ArrayList<>();
        for (String seatId : request.seatIds) {
            Ticket ticket = new Ticket();
            // Sinh mã vé ngẫu nhiên (ví dụ: T_ABC1234)
            ticket.idTicket = "T_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            ticket.idShowtime = request.idShowtime;
            ticket.idSeat = seatId;
            ticket.idPrice = showtime.idPrice;
            ticket.idBill = bill.idBill; // Gắn ID hóa đơn vừa tạo ở trên vào vé

            // Lưu vé xuống Database
            ticketRepo.persist(ticket);

            // QUAN TRỌNG NHẤT: Thêm vé vừa tạo vào danh sách để trả về Postman
            tickets.add(ticket);
        }

        // Trừ đi số ghế trống của suất chiếu
        showtime.availableSeats -= request.seatIds.size();
        return new BookingResponseDTO(bill, tickets);
    }

    public List<Movie> getMoviesByDate(String dateStr) {
        LocalDate parsedDate = LocalDate.parse(dateStr);

        // 2. Lấy mốc thời gian bắt đầu và kết thúc của ngày hôm đó
        LocalDateTime startOfDay = parsedDate.atStartOfDay(); // Ví dụ: 2025-12-15T00:00:00
        LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59); // Ví dụ: 2025-12-15T23:59:59

        // 3. Truy vấn tìm lịch chiếu nằm trong khoảng thời gian này (Không cần dùng CAST)
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

    // Xem chi tiết 1 Hóa đơn và các Vé đi kèm
    public BookingResponseDTO getBillDetail(String idBill) {
        Bill bill = billRepo.findById(idBill);
        if (bill == null) throw new RuntimeException("Không tìm thấy hóa đơn");

        List<Ticket> tickets = ticketRepo.find("idBill", idBill).list();
        return new BookingResponseDTO(bill, tickets);
    }


    // Xem lịch sử đặt vé của một Tài khoản
    public List<Bill> getMyBookingHistory() {
        // Tự động giải mã token để lấy ID
        String currentAccountId = jwt.getClaim("idAccount");
        if (currentAccountId == null) {
            throw new RuntimeException("Không thể xác thực danh tính.");
        }
        // Truy vấn dựa trên ID vừa lấy được
        return billRepo.find("idAccount", currentAccountId).list();
    }


    // Hủy vé (Xóa Bill, Ticket, cập nhật lại trạng thái Ghế và Số lượng ghế)
    @Transactional
    public void cancelBill(String idBill) {
        Bill bill = billRepo.findById(idBill);
        if (bill == null) throw new RuntimeException("Hóa đơn không tồn tại");

        // 1. Lấy danh sách vé thuộc hóa đơn này
        List<Ticket> tickets = ticketRepo.find("idBill", idBill).list();
        if (tickets.isEmpty()) throw new RuntimeException("Không tìm thấy vé trong hóa đơn");

        // Lấy thông tin lịch chiếu (để hoàn trả số ghế)
        String idShowtime = tickets.get(0).idShowtime;
        Showtime showtime = showtimeRepo.findById(idShowtime);

        for (Ticket tk : tickets) {
            // 2. Xóa trạng thái đặt ghế trong bảng showtime_seat
            ShowtimeSeatId seatId = new ShowtimeSeatId(tk.idShowtime, tk.idSeat);
            showtimeSeatRepo.deleteById(seatId);

            // 3. Xóa vé
            ticketRepo.delete(tk);
        }

        // 4. Cập nhật lại số ghế trống của lịch chiếu
        if (showtime != null) {
            showtime.availableSeats += tickets.size();
        }

        billRepo.delete(bill);
    }

    // [ADMIN] Lấy toàn bộ danh sách hóa đơn của tất cả khách hàng
    public List<Bill> getAllBillsForAdmin() {
        // Trả về toàn bộ danh sách hóa đơn, sắp xếp theo thời gian mới nhất lên đầu
        return billRepo.find("order by bookingTime desc").list();
    }
}