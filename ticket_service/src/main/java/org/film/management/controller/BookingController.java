package org.film.management.controller;

import org.film.management.dto.BookingRequest;
import org.film.management.dto.SeatStatusDTO;
import org.film.management.entity.*;
import org.film.management.repository.*;
import org.film.management.dto.BookingResponseDTO;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingController {

    @Inject ShowtimeRepo showtimeRepo;
    @Inject SeatRepo seatRepo;
    @Inject ShowtimeSeatRepo showtimeSeatRepo;
    @Inject PriceRepo priceRepo;
    @Inject BillRepo billRepo;
    @Inject TicketRepo ticketRepo;

    // =========================================================================
    // API 1: Lấy danh sách lịch chiếu theo ID Phim (Khi user click vào Phim)
    // =========================================================================
    @GET
    @Path("/showtimes/movie/{idMovie}")
    public Response getShowtimesByMovie(@PathParam("idMovie") String idMovie) {
        // Tìm tất cả lịch chiếu có idMovie tương ứng
        List<Showtime> showtimes = showtimeRepo.find("idMovie", idMovie).list();
        if (showtimes.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Không có lịch chiếu cho phim này").build();
        }
        return Response.ok(showtimes).build();
    }

    // =========================================================================
    // API 2: Lấy danh sách ghế & trạng thái (Khi user click vào Lịch chiếu)
    // =========================================================================
    @GET
    @Path("/showtimes/{idShowtime}/seats")
    public Response getSeatStatus(@PathParam("idShowtime") String idShowtime) {
        // Lấy tất cả ghế trong rạp (Từ bảng Seat)
        List<Seat> allSeats = seatRepo.listAll();

        // Lấy danh sách các ghế ĐÃ ĐƯỢC ĐẶT của lịch chiếu này (Từ bảng showtime_seat)
        List<ShowtimeSeat> bookedSeats = showtimeSeatRepo.find("idShowtime", idShowtime).list();
        List<String> bookedSeatIds = bookedSeats.stream()
                .map(bs -> bs.idSeat)
                .collect(Collectors.toList());

        // Phân loại ghế Trống (AVAILABLE) và ghế Đã đặt (BOOKED)
        List<SeatStatusDTO> seatStatuses = new ArrayList<>();
        for (Seat seat : allSeats) {
            String status = bookedSeatIds.contains(seat.idSeat) ? "BOOKED" : "AVAILABLE";
            seatStatuses.add(new SeatStatusDTO(seat.idSeat, seat.typeSeat.toString(), status));
        }

        // Frontend nhận List này, nếu status là BOOKED thì bôi xám/ẩn nút chọn ghế đi
        return Response.ok(seatStatuses).build();
    }

    // =========================================================================
    // API 3: Thực hiện đặt vé (Tính tiền, tạo Bill, tạo Ticket)
    // =========================================================================
    @POST
    @Path("/checkout")
    @Transactional
    public Response bookTickets(BookingRequest request) {

        // 1. Kiểm tra Lịch chiếu có tồn tại không
        Showtime showtime = showtimeRepo.findById(request.idShowtime);
        if (showtime == null) return Response.status(Response.Status.BAD_REQUEST).entity("Lịch chiếu không hợp lệ").build();

        // 2. Lấy thông tin Bảng giá (Price) của lịch chiếu
        Price priceConfig = priceRepo.findById(showtime.idPrice);
        int totalAmount = 0;

        // 3. Xử lý từng ghế khách chọn
        for (String seatId : request.seatIds) {
            // Kiểm tra xem ghế đã bị ai đó nhanh tay đặt mất chưa
            ShowtimeSeat checkSeat = showtimeSeatRepo.findById(new ShowtimeSeatId(request.idShowtime, seatId));
            if (checkSeat != null && "BOOKED".equals(checkSeat.status)) {
                return Response.status(Response.Status.CONFLICT).entity("Ghế " + seatId + " đã có người đặt!").build();
            }

            // Tính tiền dựa vào loại ghế (STANDARD, VIP, TRIPLE)
            Seat seat = seatRepo.findById(seatId);
            if (seat.typeSeat.name().equals("STANDARD")) totalAmount += priceConfig.standardPrice;
            else if (seat.typeSeat.name().equals("VIP")) totalAmount += priceConfig.vipPrice;
            else if (seat.typeSeat.name().equals("TRIPLE")) totalAmount += priceConfig.triplePrice;

            // Lưu trạng thái ghế vào showtime_seat là BOOKED
            ShowtimeSeat newBookedSeat = new ShowtimeSeat();
            newBookedSeat.idShowtime = request.idShowtime;
            newBookedSeat.idSeat = seatId;
            newBookedSeat.status = "BOOKED";
            showtimeSeatRepo.persist(newBookedSeat);
        }

        // 4. Tạo Hóa Đơn (Bill)
        Bill bill = new Bill();
        bill.idBill = "B" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // Tự render mã Hóa đơn
        bill.idAccount = request.idAccount;
        bill.bookingTime = LocalDateTime.now();
        bill.totalAmount = totalAmount;
        billRepo.persist(bill);

        // 5. Tạo Vé (Ticket) cho từng ghế
        List<Ticket> generatedTickets = new ArrayList<>();
        for (String seatId : request.seatIds) {
            Ticket ticket = new Ticket();
            ticket.idTicket = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // Render mã vé
            ticket.idShowtime = request.idShowtime;
            ticket.idSeat = seatId;
            ticket.idPrice = showtime.idPrice;
            ticket.idBill = bill.idBill;

            ticketRepo.persist(ticket);
            generatedTickets.add(ticket);
        }

        // 6. Cập nhật số ghế trống của lịch chiếu
        showtime.availableSeats = showtime.availableSeats - request.seatIds.size();

        // Trả về thông tin Hóa đơn và các Vé đã tạo thành công
        BookingResponseDTO responseData = new BookingResponseDTO(bill, generatedTickets);
        return Response.ok(responseData).build();
    }
}