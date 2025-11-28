package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "tickers")
@Data
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tickerId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "symbol", nullable = false, length = 20, unique = true)
    private String symbol;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

}