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
    @Column(name = "ticker_id")
    private Long tickerId;

    @Column(name = "ticker_date", nullable = false)
    private LocalDate tickerDate;

    @Column(name = "ticker_symbol", nullable = false, unique = true, length = 255)
    private String tickerSymbol;

    @Column(name = "ticker_name", nullable = false, length = 255)
    private String tickerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    @Builder.Default
    private DataSource source = DataSource.BINANCE;

    @Column(name = "ticker_type", length = 50)
    private String tickerType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

}
