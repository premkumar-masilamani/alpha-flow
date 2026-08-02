package com.alphaflow.api.controllers;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.api.services.SupportResistanceService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickers/{symbol}/support-resistances")
public class SupportResistanceController {

  private final SupportResistanceService supportResistanceService;

  public SupportResistanceController(SupportResistanceService supportResistanceService) {
    this.supportResistanceService = supportResistanceService;
  }

  @GetMapping("/daily")
  public ResponseEntity<List<SupportResistanceDto>> getDailySupportResistances(
      @PathVariable String symbol,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    List<SupportResistanceDto> data =
        supportResistanceService.getDailySupportResistances(symbol, date);
    return ResponseEntity.ok(data);
  }

  @GetMapping("/weekly")
  public ResponseEntity<List<SupportResistanceDto>> getWeeklySupportResistances(
      @PathVariable String symbol,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    List<SupportResistanceDto> data =
        supportResistanceService.getWeeklySupportResistances(symbol, date);
    return ResponseEntity.ok(data);
  }
}
