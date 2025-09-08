package com.prem.ta.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "intervals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_intervals_label", columnNames = "label")
        }
)
public class Interval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interval_id", updatable = false, nullable = false)
    private Integer intervalId;

    @Column(name = "label", nullable = false, length = 10)
    private String label;

    // Duration in minutes
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @OneToMany(mappedBy = "interval", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeData> tradeData = new ArrayList<>();

    // Getters and setters
    public Integer getIntervalId() {
        return intervalId;
    }

    public void setIntervalId(Integer intervalId) {
        this.intervalId = intervalId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<TradeData> getTradeData() {
        return tradeData;
    }

    public void setTradeData(List<TradeData> tradeData) {
        this.tradeData = tradeData;
    }
}
