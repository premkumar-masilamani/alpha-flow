package com.prem.ta.entities;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "tickers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_tickers_symbol", columnNames = "symbol")
        }
)
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticker_id", updatable = false, nullable = false)
    private Long tickerId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    // Relationships
    @OneToMany(mappedBy = "ticker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileRecord> files = new ArrayList<>();

    @OneToMany(mappedBy = "ticker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeData> tradeData = new ArrayList<>();

    public Long getTickerId() {
        return tickerId;
    }

    public void setTickerId(Long tickerId) {
        this.tickerId = tickerId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public List<FileRecord> getFiles() {
        return files;
    }

    public void setFiles(List<FileRecord> files) {
        this.files = files;
    }

    public List<TradeData> getTradeData() {
        return tradeData;
    }

    public void setTradeData(List<TradeData> tradeData) {
        this.tradeData = tradeData;
    }
}
