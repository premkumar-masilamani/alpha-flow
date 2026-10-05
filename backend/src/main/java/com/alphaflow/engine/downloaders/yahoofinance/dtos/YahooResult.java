package com.alphaflow.engine.downloaders.yahoofinance.dtos;

import java.util.List;

public record YahooResult(List<Long> timestamp, YahooIndicators indicators) {}
