package com.alphaflow.api.services;

import com.alphaflow.api.dtos.SRTouchPointDTO;
import com.alphaflow.api.dtos.SupportResistanceDTO;
import com.alphaflow.persistence.repositories.SupportResistanceRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SupportResistanceService {
  private final SupportResistanceRepository srRepo;

  public SupportResistanceService(SupportResistanceRepository srRepo) {
    this.srRepo = srRepo;
  }

  public List<SupportResistanceDTO> getSupportResistance(String tickerSymbol) {
    return srRepo
        .findByTicker_TickerSymbolAndFilterReasonIsNull(tickerSymbol.toUpperCase())
        .stream()
        .map(
            sr ->
                SupportResistanceDTO.builder()
                    .timeframe(sr.getTimeframe().name())
                    .currentType(sr.getCurrentType().name())
                    .importance(sr.getImportance())
                    .touchPoints(
                        sr.getTouchPoints().stream()
                            .map(
                                tp ->
                                    SRTouchPointDTO.builder()
                                        .date(tp.getDate())
                                        .price(tp.getPrice())
                                        .build())
                            .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }
}
