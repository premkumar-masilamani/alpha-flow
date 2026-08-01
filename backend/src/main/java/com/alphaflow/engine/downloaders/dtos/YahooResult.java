package com.alphaflow.engine.downloaders.dtos;

import java.util.List;

public record YahooResult(List<Long> timestamp, YahooIndicators indicators) {}
