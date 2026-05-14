package org.film.management.dto;

public class SeatStatusDTO {
    public String seatCode;
    public String typeSeat;
    public String status;

    public SeatStatusDTO(String seatCode, String typeSeat, String status) {
        this.seatCode = seatCode;
        this.typeSeat = typeSeat;
        this.status = status;
    }
}