package com.prem.ta.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tickers")
@Data
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tickerId;

    private String symbol;
    private String name;
    private OffsetDateTime startDate;

}
