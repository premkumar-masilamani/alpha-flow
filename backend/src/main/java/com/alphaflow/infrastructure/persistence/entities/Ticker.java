package com.alphaflow.infrastructure.persistence.entities;

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

    private LocalDate tickerDate;

    private String tickerSymbol;

    private String tickerName;

    private boolean isActive = true;

}