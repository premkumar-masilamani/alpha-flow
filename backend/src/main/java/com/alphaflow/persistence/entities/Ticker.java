package com.alphaflow.persistence.entities;


import jakarta.persistence.*;

import lombok.*;


@Entity

@Table(name = "tickers")

@Getter

@Setter

@ToString

@EqualsAndHashCode(onlyExplicitlyIncluded = true)

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class Ticker {


  @Id

  @GeneratedValue(strategy = GenerationType.IDENTITY)

  @Column(name = "ticker_id")

  private Long tickerId;


  @EqualsAndHashCode.Include

  @Column(name = "ticker_symbol", nullable = false, unique = true)

  private String tickerSymbol;


  @Column(name = "ticker_name", nullable = false)

  private String tickerName;


  @Column(name = "is_active", nullable = false)

  @Builder.Default

  private boolean isActive = true;


}

