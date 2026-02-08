package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "candles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class Candle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long candleId;

    private LocalDate candleDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id")
    private Ticker ticker;

    private BigDecimal priceOpen;

    private BigDecimal priceHigh;

    private BigDecimal priceLow;

    private BigDecimal priceClose;

    private BigDecimal volume;

    private BigDecimal vwap;

    @Column(name = "capital_poc")
    private BigDecimal capitalPOC;

    @Column(name = "capital_vah")
    private BigDecimal capitalVAH;

    @Column(name = "capital_val")
    private BigDecimal capitalVAL;

    private BigDecimal buyerCapital;

    private BigDecimal totalCapital;

    public Candle merge(Candle other) {
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
