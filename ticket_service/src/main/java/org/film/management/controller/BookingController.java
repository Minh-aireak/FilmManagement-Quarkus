package org.film.management.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.film.management.dto.BookingRequest;
import org.film.management.service.BookingService;
import io.quarkus.security.Authenticated;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingController {

    @Inject BookingService bookingService;
    // API 1: Lấy danh sách lịch chiếu theo Phim
    @GET
    @Path("/movie/showtimes/{idMovie}")
    public Response getShowtimesByMovie(@PathParam("idMovie") String idMovie) {
        var result = bookingService.getShowtimesByMovie(idMovie);
        return result.isEmpty() ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(result).build();
    }

    // API 2: Lấy danh sách ghế và trạng thái theo Lịch chiếu
    @GET
    @Path("/seats/showtimes/{idShowtime}")
    public Response getSeatStatus(@PathParam("idShowtime") String idShowtime) {
        return Response.ok(bookingService.getSeatStatus(idShowtime)).build();
    }
    // API 3: Thực hiện đặt vé
    @POST
    @Path("/booking")
    @Authenticated
    public Response bookTickets(BookingRequest request) {
        try {
            return Response.ok(bookingService.bookTickets(request)).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    // API 4: Lấy danh sách phim theo Ngày chiếu
    @GET
    @Path("/date/{date}/movies")
    public Response getMoviesByDate(@PathParam("date") String date) {
        try {
            var result = bookingService.getMoviesByDate(date);
            return result.isEmpty() ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(result).build();
        }
        catch (java.time.format.DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Sai định dạng ngày. Vui lòng nhập YYYY-MM-DD").build();
        }
        catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Lỗi hệ thống: " + e.getMessage()).build();
        }
    }
    // API 5: Lấy toàn bộ danh sách lịch chiếu
    @GET
    @Path("/showtimes")
    public Response getAllShowtimes() {
        return Response.ok(bookingService.getAllShowtimes()).build();
    }
    // API 6: Xem chi tiết 1 hóa đơn vừa đặt
    @GET
    @Path("/bills/{idBill}")
    public Response getBillDetail(@PathParam("idBill") String idBill) {
        try {
            return Response.ok(bookingService.getBillDetail(idBill)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    // API 7: Xem lịch sử đặt vé của user
    @GET
    @Path("/history")
    @Authenticated // BẮT BUỘC có Token
    public Response getHistory() {
        try {
            return Response.ok(bookingService.getMyBookingHistory()).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }

    // API 8: Hủy đặt vé
    @DELETE
    @Path("/cancel/{idBill}")
    public Response cancelBooking(@PathParam("idBill") String idBill) {
        try {
            bookingService.cancelBill(idBill);
            return Response.ok("Hủy vé thành công và đã hoàn trả ghế trống").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
    // API 9: [ADMIN] Lấy toàn bộ danh sách hóa đơn
    @GET
    @Path("/admin/bills")
    public Response getAllBills() {
        return Response.ok(bookingService.getAllBillsForAdmin()).build();
    }
}