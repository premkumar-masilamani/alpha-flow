package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "market_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long marketDataId;

    @Column(name = "market_data_date", nullable = false)
    private LocalDate marketDataDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "price_open", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceOpen;

    @Column(name = "price_high", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceHigh;

    @Column(name = "price_low", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceLow;

    @Column(name = "price_close", nullable = false, precision = 38, scale = 2)
    private BigDecimal priceClose;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal volume;

    @Column(precision = 38, scale = 2)
    private BigDecimal vwap;

    @Column(name = "capital_poc", precision = 38, scale = 2)
    private BigDecimal capitalPOC;

    @Column(name = "capital_vah", precision = 38, scale = 2)
    private BigDecimal capitalVAH;

    @Column(name = "capital_val", precision = 38, scale = 2)
    private BigDecimal capitalVAL;

    @Column(name = "buyer_capital", precision = 38, scale = 2)
    private BigDecimal buyerCapital;

    @Column(name = "total_capital", precision = 38, scale = 2)
    private BigDecimal totalCapital;

    public MarketData merge(MarketData other) {
        this.priceOpen = other.priceOpen;
        this.priceHigh = other.priceHigh;
        this.priceLow = other.priceLow;
        this.priceClose = other.priceClose;
        this.volume = other.volume;
        this.vwap = other.vwap;
        this.capitalPOC = other.capitalPOC;
        this.capitalVAH = other.capitalVAH;
        this.capitalVAL = other.capitalVAL;
        this.buyerCapital = other.buyerCapital;
        this.totalCapital = other.totalCapital;
        return this;
    }

}
