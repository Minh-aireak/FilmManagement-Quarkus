package org.film.management.entity;

import java.io.Serializable;
import java.util.Objects;

public class ShowtimeSeatId implements Serializable {
    public String idShowtime;
    public String idSeat;

    public ShowtimeSeatId() {}

    public ShowtimeSeatId(String idShowtime, String idSeat) {
        this.idShowtime = idShowtime;
        this.idSeat = idSeat;
    }

    // Bắt buộc phải có equals và hashCode cho khóa kép
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShowtimeSeatId that = (ShowtimeSeatId) o;
        return Objects.equals(idShowtime, that.idShowtime) &&
                Objects.equals(idSeat, that.idSeat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idShowtime, idSeat);
    }
}