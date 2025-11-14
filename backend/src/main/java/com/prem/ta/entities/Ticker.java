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
    private Integer tickerId;

    private String symbol;

    private String name;

    private OffsetDateTime startDate;

    @OneToMany(mappedBy = "ticker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileRecord> files;

    @OneToMany(mappedBy = "ticker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeData> trades;
}
