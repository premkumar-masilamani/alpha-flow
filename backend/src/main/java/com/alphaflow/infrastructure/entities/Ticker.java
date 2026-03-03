package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tickers")
@Data
@Getter
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

    @Column(name = "ticker_symbol", nullable = false, unique = true)
    private String tickerSymbol;

    @Column(name = "ticker_name", nullable = false)
    private String tickerName;

    @Column(name = "ticker_type", length = 50)
    private String tickerType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

}
