package com.alphaflow.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "candle_data")
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
public class CandleData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candle_data_id")
    private Long candleDataId;

    @Column(name = "candle_date", nullable = false)
    private LocalDate candleDataDate;

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

    @Column(name = "volume", nullable = false, precision = 38, scale = 2)
    private BigDecimal volume;

    @Column(name = "vwap", precision = 38, scale = 2)
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

}
