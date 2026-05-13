package org.film.management.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.film.management.dto.BookingRequest;
import org.film.management.service.BookingService;
import io.quarkus.security.Authenticated;

import java.util.List;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingController {

    @Inject BookingService bookingService;
    @GET
    @Path("/movies/showtimes/{idMovie}")
    public Response getShowtimesByMovie(@PathParam("idMovie") String idMovie) {
        var result = bookingService.getShowtimesByMovie(idMovie);
        return result.isEmpty() ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(result).build();
    }

    @GET
    @Path("/seats/showtimes/{idShowtime}")
    public Response getBookedSeats(@PathParam("idShowtime") String idShowtime) {
        List<String> bookedSeats = bookingService.getBookedSeats(idShowtime);

        return Response.ok(bookedSeats).build();
    }

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

    @GET
    @Path("/date/{date}/movies")
    public Response getMoviesByDate(@PathParam("date") String date) {
        try {
            var result = bookingService.getMoviesByDate(date);

            if (result.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Không có suất chiếu nào trong ngày " + date).build();
            }

            return Response.ok(result).build();
        }
        catch (java.time.format.DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sai định dạng ngày. Vui lòng nhập YYYY-MM-DD").build();
        }
        catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Lỗi hệ thống: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/showtimes")
    public Response getAllShowtimes() {
        return Response.ok(bookingService.getAllShowtimes()).build();
    }

    @GET
    @Path("/bills/{idBill}")
    @Authenticated
    public Response getBillDetail(@PathParam("idBill") String idBill) {
        try {
            return Response.ok(bookingService.getBillDetail(idBill)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/history")
    @Authenticated
    public Response getHistory() {
        try {
            return Response.ok(bookingService.getMyBookingHistory()).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/cancel/{idBill}")
    @RolesAllowed({"ADMIN", "CUSTOMER"})
    public Response cancelBooking(@PathParam("idBill") String idBill) {
        try {
            bookingService.cancelBill(idBill);
            return Response.ok("Hủy vé thành công và đã hoàn trả ghế trống").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/admin/bills")
    @RolesAllowed("ADMIN")
    public Response getAllBills() {
        return Response.ok(bookingService.getAllBillsForAdmin()).build();
    }
}