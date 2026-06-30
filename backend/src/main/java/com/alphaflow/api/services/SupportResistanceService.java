package com.alphaflow.api.services;

import com.alphaflow.api.dtos.SupportResistanceDTO;
import com.alphaflow.api.dtos.SRTouchPointDTO;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SupportResistanceService {
  private final DailySupportResistanceRepository dailySrRepo;
  private final WeeklySupportResistanceRepository weeklySrRepo;

  public SupportResistanceService(
      DailySupportResistanceRepository dailySrRepo,
      WeeklySupportResistanceRepository weeklySrRepo) {
    this.dailySrRepo = dailySrRepo;
    this.weeklySrRepo = weeklySrRepo;
  }

  public List<SupportResistanceDTO> getDailySr(String tickerSymbol) {
    return dailySrRepo.findByTicker_TickerSymbolAndFilterReasonIsNull(tickerSymbol.toUpperCase()).stream()
        .map(
            sr ->
                SupportResistanceDTO.builder()
                    .currentType(sr.getCurrentType().name())
                    .importance(sr.getImportance())
                    .touchPoints(sr.getTouchPoints().stream()
                        .map(tp -> SRTouchPointDTO.builder().date(tp.getDate()).price(tp.getPrice()).build())
                        .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }

  public List<SupportResistanceDTO> getWeeklySr(String tickerSymbol) {
    return weeklySrRepo.findByTicker_TickerSymbolAndFilterReasonIsNull(tickerSymbol.toUpperCase()).stream()
        .map(
            sr ->
                SupportResistanceDTO.builder()
                    .currentType(sr.getCurrentType().name())
                    .importance(sr.getImportance())
                    .touchPoints(sr.getTouchPoints().stream()
                        .map(tp -> SRTouchPointDTO.builder().date(tp.getDate()).price(tp.getPrice()).build())
                        .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }
}
