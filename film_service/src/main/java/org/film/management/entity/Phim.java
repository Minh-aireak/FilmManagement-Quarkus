package org.film.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "phim")
public class Phim {
    @Id
    private String idPhim;
    private String tenPhim;
    private String tacGia;
    private String dienVien;
    private String thoiLuong;
    private String ngonNgu;
    private String moTa;
    private String anhPhim;
}