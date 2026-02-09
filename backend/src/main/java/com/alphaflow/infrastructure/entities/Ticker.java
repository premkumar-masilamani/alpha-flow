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

    @Column(name = "ticker_date", nullable = false)
    private LocalDate tickerDate;

    @Column(name = "ticker_symbol", unique = true, nullable = false)
    private String tickerSymbol;

    @Column(name = "ticker_name", nullable = false)
    private String tickerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DataSource source = DataSource.BINANCE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

}
