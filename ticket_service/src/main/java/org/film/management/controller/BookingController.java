package org.film.management.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.film.management.dto.AddShowtimeRequest;
import org.film.management.dto.BookingRequest;
import org.film.management.entity.Showtime;
import org.film.management.service.BookingService;
import io.quarkus.security.Authenticated;

import java.util.List;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingController {

    @Inject BookingService bookingService;

    @GET
    @Path("/bookings/seats/{idShowtime}")
    public Response getBookedSeats(@PathParam("idShowtime") String idShowtime) {
        List<String> bookedSeats = bookingService.getBookedSeats(idShowtime);

        return Response.ok(bookedSeats).build();
    }

    @POST
    @Path("/bookings")
    @Authenticated
    public Response bookTickets(BookingRequest request) {
        try {
            return Response.ok(bookingService.bookTickets(request)).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/tickets/bills/{idBill}")
    @Authenticated
    public Response getBillDetail(@PathParam("idBill") String idBill) {
        try {
            return Response.ok(bookingService.getBillDetail(idBill)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/tickets/history")
    @Authenticated
    public Response getHistory() {
        try {
            return Response.ok(bookingService.getMyBookingHistory()).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/tickets/cancel/{idBill}")
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
    @Path("/tickets/admin/bills")
    @RolesAllowed("ADMIN")
    public Response getAllBills() {
        return Response.ok(bookingService.getAllBillsForAdmin()).build();
    }

    @POST
    @Path("/admin/showtimes")
    @RolesAllowed("ADMIN")
    public Response addShowtime(AddShowtimeRequest request) {
        try {
            Showtime created = bookingService.addShowtime(request);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Lỗi khi tạo lịch chiếu: " + e.getMessage()).build();
        }
    }
}