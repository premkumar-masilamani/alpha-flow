package com.alphaflow.persistence.repositories;


import com.alphaflow.persistence.entities.IndicatorState;

import com.alphaflow.persistence.entities.Ticker;

import com.alphaflow.persistence.enums.Timeframe;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.List;


/**

 * Per-combo resume checkpoints. See docs/indicators-design.md.

 */

@Repository

public interface IndicatorStateRepository extends JpaRepository<IndicatorState, Long> {


  /**

   * All checkpoints for a ticker on a timeframe (loaded once per processing run).

   */

  List<IndicatorState> findByTickerAndTimeframe(Ticker ticker, Timeframe timeframe);

}

