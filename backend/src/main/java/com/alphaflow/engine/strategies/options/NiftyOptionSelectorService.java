package com.alphaflow.engine.strategies.options;

import com.alphaflow.engine.calendar.MarketTradingCalendar;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.AngelOneScripMasterManager;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneInstrument;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.engine.strategies.options.dtos.RecommendedOptionDto;
import com.alphaflow.engine.strategies.options.enums.OptionType;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NiftyOptionSelectorService {

  public static final BigDecimal MIN_PREMIUM = new BigDecimal("500");
  public static final BigDecimal MAX_PREMIUM = new BigDecimal("900");
  public static final long MIN_DTE = 10L;
  public static final long MAX_DTE = 16L;
  private static final BigDecimal CALL_LOWER_OFFSET = new BigDecimal("1200");
  private static final BigDecimal CALL_UPPER_OFFSET = new BigDecimal("200");
  private static final BigDecimal PUT_LOWER_OFFSET = new BigDecimal("200");
  private static final BigDecimal PUT_UPPER_OFFSET = new BigDecimal("1200");
  private static final int ANGEL_ONE_MAX_QUOTE_TOKENS_LIMIT = 50;

  private final AngelOneScripMasterManager scripMasterManager;
  private final AngelOneClient angelOneClient;

  @Autowired
  public NiftyOptionSelectorService(
      AngelOneScripMasterManager scripMasterManager, AngelOneClient angelOneClient) {
    this.scripMasterManager = scripMasterManager;
    this.angelOneClient = angelOneClient;
  }

  public Optional<RecommendedOptionDto> selectBestOption(
      Ticker ticker, BigDecimal spotPrice, OptionType optionType, LocalDate sessionDate) {
    if (isEligibleNiftyEvaluation(ticker, sessionDate)) {
      List<AngelOneInstrument> allNfoOptions = scripMasterManager.getNfoOptions("NIFTY", sessionDate);
      List<AngelOneInstrument> typeMatchingInstruments = filterByOptionType(allNfoOptions, optionType);

      if (!typeMatchingInstruments.isEmpty()) {
        Optional<LocalDate> targetExpiryOptional =
            findTargetExpiryDate(typeMatchingInstruments, sessionDate);

        if (targetExpiryOptional.isPresent()) {
          LocalDate targetExpiry = targetExpiryOptional.get();
          long daysToExpiry = ChronoUnit.DAYS.between(sessionDate, targetExpiry);

          List<AngelOneInstrument> candidateInstruments =
              filterCandidateInstruments(typeMatchingInstruments, targetExpiry, spotPrice, optionType);

          return evaluateCandidatesAndSelectBest(
              candidateInstruments, spotPrice, optionType, targetExpiry, daysToExpiry);
        } else {
          log.info(
              "No active NIFTY expiry found within the {}-{} DTE window from {}",
              MIN_DTE,
              MAX_DTE,
              sessionDate);
          return Optional.empty();
        }
      } else {
        log.warn("No NIFTY option instruments available in scrip master for option type {}", optionType);
        return Optional.empty();
      }
    } else {
      log.debug("Skipping option selection: ticker {} on date {} is not eligible for NIFTY options selection.",
          ticker != null ? ticker.getTickerSymbol() : "null", sessionDate);
      return Optional.empty();
    }
  }

  private boolean isEligibleNiftyEvaluation(Ticker ticker, LocalDate sessionDate) {
    if (ticker != null && isNiftySymbol(ticker.getTickerSymbol())) {
      return MarketTradingCalendar.isTradingDay(sessionDate, Country.IN);
    } else {
      return false;
    }
  }

  private boolean isNiftySymbol(String symbol) {
    if (symbol != null) {
      String cleanSymbol = symbol.trim().replace(" ", "").toUpperCase(Locale.ROOT);
      return "NIFTY50".equals(cleanSymbol);
    } else {
      return false;
    }
  }

  private List<AngelOneInstrument> filterByOptionType(
      List<AngelOneInstrument> instruments, OptionType optionType) {
    String suffix = optionType.getCode();
    List<AngelOneInstrument> filtered = new ArrayList<>();
    for (AngelOneInstrument instrument : instruments) {
      if (instrument.symbol().toUpperCase(Locale.ROOT).endsWith(suffix)) {
        filtered.add(instrument);
      }
    }
    return filtered;
  }

  private Optional<LocalDate> findTargetExpiryDate(
      List<AngelOneInstrument> instruments, LocalDate sessionDate) {
    return instruments.stream()
        .map(AngelOneInstrument::expiry)
        .distinct()
        .filter(
            expiry -> {
              long dte = ChronoUnit.DAYS.between(sessionDate, expiry);
              return dte >= MIN_DTE && dte <= MAX_DTE;
            })
        .min(Comparator.naturalOrder());
  }

  private List<AngelOneInstrument> filterCandidateInstruments(
      List<AngelOneInstrument> instruments,
      LocalDate targetExpiry,
      BigDecimal spotPrice,
      OptionType optionType) {
    List<AngelOneInstrument> expiryMatches = new ArrayList<>();
    for (AngelOneInstrument instrument : instruments) {
      if (targetExpiry.equals(instrument.expiry())) {
        expiryMatches.add(instrument);
      }
    }

    BigDecimal minStrike;
    BigDecimal maxStrike;
    if (optionType == OptionType.CALL) {
      minStrike = spotPrice.subtract(CALL_LOWER_OFFSET);
      maxStrike = spotPrice.add(CALL_UPPER_OFFSET);
    } else {
      minStrike = spotPrice.subtract(PUT_LOWER_OFFSET);
      maxStrike = spotPrice.add(PUT_UPPER_OFFSET);
    }

    List<AngelOneInstrument> corridorMatches = new ArrayList<>();
    for (AngelOneInstrument instrument : expiryMatches) {
      BigDecimal strike = instrument.strike();
      if (strike.compareTo(minStrike) >= 0 && strike.compareTo(maxStrike) <= 0) {
        corridorMatches.add(instrument);
      }
    }

    List<AngelOneInstrument> candidateList =
        !corridorMatches.isEmpty() ? corridorMatches : expiryMatches;

    if (candidateList.size() > ANGEL_ONE_MAX_QUOTE_TOKENS_LIMIT) {
      candidateList.sort(
          Comparator.comparing(
              candidate -> candidate.strike().subtract(spotPrice).abs()));
      return candidateList.subList(0, ANGEL_ONE_MAX_QUOTE_TOKENS_LIMIT);
    } else {
      return candidateList;
    }
  }

  private Optional<RecommendedOptionDto> evaluateCandidatesAndSelectBest(
      List<AngelOneInstrument> candidateInstruments,
      BigDecimal spotPrice,
      OptionType optionType,
      LocalDate targetExpiry,
      long daysToExpiry) {
    List<String> tokens =
        candidateInstruments.stream().map(AngelOneInstrument::token).toList();

    Map<String, AngelOneQuote> liveQuotes =
        angelOneClient.getMarketQuotes(MarketConfig.EXCHANGE_SEGMENT_NFO, tokens);

    if (!liveQuotes.isEmpty()) {
      AngelOneInstrument bestInstrument = null;
      BigDecimal bestLtp = null;
      BigDecimal bestIntrinsicValue = null;
      BigDecimal bestTimeValue = null;

      for (AngelOneInstrument candidate : candidateInstruments) {
        AngelOneQuote quote = liveQuotes.get(candidate.token());
        BigDecimal ltp = quote != null ? quote.lastPrice() : null;
        if (ltp != null && ltp.compareTo(MIN_PREMIUM) >= 0 && ltp.compareTo(MAX_PREMIUM) <= 0) {
          BigDecimal intrinsicValue = computeIntrinsicValue(spotPrice, candidate.strike(), optionType);
          BigDecimal timeValue = ltp.subtract(intrinsicValue).setScale(4, RoundingMode.HALF_UP);

          if (bestTimeValue == null
              || timeValue.compareTo(bestTimeValue) < 0
              || (timeValue.compareTo(bestTimeValue) == 0
                  && candidate.strike().compareTo(bestInstrument.strike()) < 0)) {
            bestInstrument = candidate;
            bestLtp = ltp.setScale(4, RoundingMode.HALF_UP);
            bestIntrinsicValue = intrinsicValue.setScale(4, RoundingMode.HALF_UP);
            bestTimeValue = timeValue;
          }
        }
      }

      if (bestInstrument != null) {
        return Optional.of(
            new RecommendedOptionDto(
                bestInstrument.symbol(),
                bestInstrument.token(),
                bestInstrument.strike(),
                optionType,
                targetExpiry,
                daysToExpiry,
                bestLtp,
                bestIntrinsicValue,
                bestTimeValue));
      } else {
        log.info(
            "No option quotes found meeting the {}-{} premium range for expiry {}",
            MIN_PREMIUM,
            MAX_PREMIUM,
            targetExpiry);
        return Optional.empty();
      }
    } else {
      log.warn("Failed to retrieve live market quotes for candidate tokens: {}", tokens);
      return Optional.empty();
    }
  }

  private BigDecimal computeIntrinsicValue(
      BigDecimal spotPrice, BigDecimal strikePrice, OptionType optionType) {
    BigDecimal rawDifference;
    if (optionType == OptionType.CALL) {
      rawDifference = spotPrice.subtract(strikePrice);
    } else {
      rawDifference = strikePrice.subtract(spotPrice);
    }

    if (rawDifference.compareTo(BigDecimal.ZERO) > 0) {
      return rawDifference;
    } else {
      return BigDecimal.ZERO;
    }
  }
}
