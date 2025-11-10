package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "tickers")
@Getter
@Setter
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticker_id")
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @OneToMany(mappedBy = "ticker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileRecord> files;

    @OneToMany(mappedBy = "ticker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeData> trades;
}
