  private List<ActiveLine> compute(List<PriceBar> bars, int window) {
    List<ActiveLine> activeLines = new ArrayList<>();
    if (bars.size() < window * 2) return activeLines;

    List<Pivot> pivotHighs = new ArrayList<>();
    List<Pivot> pivotLows = new ArrayList<>();
    BigDecimal tolerance = new BigDecimal("0.01"); // 1%

    LocalDate twoYearsAgo = bars.get(bars.size() - 1).date().minusYears(2);
    int startIndex = window;
    for (int i = bars.size() - 1; i >= 0; i--) {
        if (bars.get(i).date().isBefore(twoYearsAgo)) {
            startIndex = Math.max(window, i);
            break;
        }
    }

    for (int i = startIndex; i < bars.size() - window; i++) {
      boolean isHigh = true;
      boolean isLow = true;
      BigDecimal currentHigh = bars.get(i).high();
      BigDecimal currentLow = bars.get(i).low();

      for (int j = i - window; j <= i + window; j++) {
        if (j == i) continue;
        if (bars.get(j).high().compareTo(currentHigh) > 0) isHigh = false;
        if (bars.get(j).low().compareTo(currentLow) < 0) isLow = false;
      }

      if (isHigh) {
        Pivot currentPivot = new Pivot(i, currentHigh, bars.get(i).date());
        
        // 1. Horizontal logic
        boolean addedToExisting = false;
        for (ActiveLine line : activeLines) {
            if (line.slope.compareTo(BigDecimal.ZERO) == 0 && line.type == SRCurrentType.RESISTANCE) {
                BigDecimal avgPrice = line.intercept;
                BigDecimal diff = currentHigh.subtract(avgPrice).abs();
                BigDecimal thresh = avgPrice.multiply(tolerance);
                if (diff.compareTo(thresh) <= 0) {
                    line.touchPoints.add(new SRTouchPoint(currentPivot.date, avgPrice));
                    line.importance = line.touchPoints.size();
                    addedToExisting = true;
                    break;
                }
            }
        }
        
        if (!addedToExisting) {
            for (Pivot past : pivotHighs) {
              BigDecimal diff = currentHigh.subtract(past.price).abs();
              BigDecimal thresh = past.price.multiply(tolerance);
              if (diff.compareTo(thresh) <= 0) {
                BigDecimal avgPrice = currentHigh.add(past.price).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
                List<SRTouchPoint> pts = new ArrayList<>();
                pts.add(new SRTouchPoint(past.date, avgPrice));
                pts.add(new SRTouchPoint(currentPivot.date, avgPrice));
                activeLines.add(new ActiveLine(pts, SRCurrentType.RESISTANCE, BigDecimal.ZERO, avgPrice));
                break;
              }
            }
        }
        
        // 2. Angular logic
        pivotHighs.add(currentPivot);
        if (pivotHighs.size() >= 4) {
          List<Pivot> last4 = pivotHighs.subList(pivotHighs.size() - 4, pivotHighs.size());
          ActiveLine angularLine = createRegressionLine(last4, SRCurrentType.RESISTANCE);
          if (angularLine != null) {
            activeLines.add(angularLine);
          }
        }
      }

      if (isLow) {
        Pivot currentPivot = new Pivot(i, currentLow, bars.get(i).date());
        
        // 1. Horizontal logic
        boolean addedToExisting = false;
        for (ActiveLine line : activeLines) {
            if (line.slope.compareTo(BigDecimal.ZERO) == 0 && line.type == SRCurrentType.SUPPORT) {
                BigDecimal avgPrice = line.intercept;
                BigDecimal diff = currentLow.subtract(avgPrice).abs();
                BigDecimal thresh = avgPrice.multiply(tolerance);
                if (diff.compareTo(thresh) <= 0) {
                    line.touchPoints.add(new SRTouchPoint(currentPivot.date, avgPrice));
                    line.importance = line.touchPoints.size();
                    addedToExisting = true;
                    break;
                }
            }
        }
        
        if (!addedToExisting) {
            for (Pivot past : pivotLows) {
              BigDecimal diff = currentLow.subtract(past.price).abs();
              BigDecimal thresh = past.price.multiply(tolerance);
              if (diff.compareTo(thresh) <= 0) {
                BigDecimal avgPrice = currentLow.add(past.price).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
                List<SRTouchPoint> pts = new ArrayList<>();
                pts.add(new SRTouchPoint(past.date, avgPrice));
                pts.add(new SRTouchPoint(currentPivot.date, avgPrice));
                activeLines.add(new ActiveLine(pts, SRCurrentType.SUPPORT, BigDecimal.ZERO, avgPrice));
                break;
              }
            }
        }
        
        // 2. Angular logic
        pivotLows.add(currentPivot);
        if (pivotLows.size() >= 4) {
          List<Pivot> last4 = pivotLows.subList(pivotLows.size() - 4, pivotLows.size());
          ActiveLine angularLine = createRegressionLine(last4, SRCurrentType.SUPPORT);
          if (angularLine != null) {
            activeLines.add(angularLine);
          }
        }
      }

      // Check breaks
      BigDecimal close = bars.get(i).close();
      Iterator<ActiveLine> it = activeLines.iterator();
      while (it.hasNext()) {
        ActiveLine line = it.next();
        
        // Check if the line was formed in the future relative to current bar
        boolean isFuture = false;
        for (SRTouchPoint tp : line.touchPoints) {
            if (tp.getDate().isAfter(bars.get(i).date()) || tp.getDate().isEqual(bars.get(i).date())) {
                isFuture = true;
                break;
            }
        }
        if (isFuture) continue;

        BigDecimal expectedPrice = line.slope.multiply(new BigDecimal(i)).add(line.intercept);

        if (line.type == SRCurrentType.RESISTANCE && close.compareTo(expectedPrice) > 0) {
          line.breakCount++;
          if (line.breakCount > 2) {
            it.remove();
          } else {
            line.type = SRCurrentType.SUPPORT; // Flip polarity
          }
        } else if (line.type == SRCurrentType.SUPPORT && close.compareTo(expectedPrice) < 0) {
          line.breakCount++;
          if (line.breakCount > 2) {
            it.remove();
          } else {
            line.type = SRCurrentType.RESISTANCE; // Flip polarity
          }
        }
      }
    }

    return activeLines.stream()
        .filter(al -> al.slope.compareTo(BigDecimal.ZERO) == 0 ? al.touchPoints.size() >= 3 : al.touchPoints.size() >= 4)
        .toList();
  }
