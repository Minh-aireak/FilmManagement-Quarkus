package org.film.management.dto;

import java.time.LocalDateTime;

public class AddShowtimeRequest {
    public String idMovie;
    public String idRoom;
    public LocalDateTime showTime;

    public int standardPrice;
    public int vipPrice;
    public int couplePrice;
}
