package com.alphaflow.infrastructure.entities;

import com.alphaflow.infrastructure.enums.DataSource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tickers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tickerId;

    private LocalDate tickerDate;

    private String tickerSymbol;

    private String tickerName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DataSource source = DataSource.BINANCE;

    private boolean isActive = true;

}