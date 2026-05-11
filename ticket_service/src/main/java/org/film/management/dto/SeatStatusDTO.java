package org.film.management.dto;

public class SeatStatusDTO {
    public String idSeat;
    public String typeSeat; // STANDARD, VIP, TRIPLE
    public String status;   // AVAILABLE (Trống), BOOKED (Đã đặt)

    public SeatStatusDTO(String idSeat, String typeSeat, String status) {
        this.idSeat = idSeat;
        this.typeSeat = typeSeat;
        this.status = status;
    }
}
