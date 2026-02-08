package com.alphaflow.backtest.engine;

import com.alphaflow.backtest.enums.PositionType;

import java.math.BigDecimal;

import static com.alphaflow.infrastructure.constants.AppConstants.DB_MATH_CONTEXT;

public class Portfolio {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private BigDecimal cash;
    private BigDecimal quantity;
    private BigDecimal entryPrice;
    private PositionType positionType;

    public Portfolio(BigDecimal initialEquity) {
        if (initialEquity == null) {
            throw new IllegalArgumentException("Initial equity is required");
        }
        this.cash = initialEquity;
        this.quantity = BigDecimal.ZERO;
        this.entryPrice = BigDecimal.ZERO;
        this.positionType = PositionType.NONE;
    }

    public PositionType getPositionType() {
        return positionType;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public BigDecimal equityAtPrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price is required");
        }
        BigDecimal equity = switch (positionType) {
            case LONG -> cash.add(quantity.multiply(price));
            case SHORT -> cash.add(quantity.multiply(entryPrice.subtract(price)));
            default -> cash;
        };
        if (equity.signum() < 0) {
            throw new IllegalStateException("Negative equity: " + equity);
        }
        return equity;
    }

    public void enterLong(BigDecimal price, BigDecimal equityAtOpen) {
        ensurePositivePrice(price);
        ensureNonNegativeEquity(equityAtOpen);
        quantity = equityAtOpen.divide(price, DB_MATH_CONTEXT);
        cash = BigDecimal.ZERO;
        entryPrice = price;
        positionType = PositionType.LONG;
    }

    public void enterShort(BigDecimal price, BigDecimal equityAtOpen) {
        ensurePositivePrice(price);
        ensureNonNegativeEquity(equityAtOpen);
        quantity = equityAtOpen.divide(price, DB_MATH_CONTEXT);
        cash = equityAtOpen;
        entryPrice = price;
        positionType = PositionType.SHORT;
    }

    public void exitToCash(BigDecimal equityAtOpen) {
        ensureNonNegativeEquity(equityAtOpen);
        cash = equityAtOpen;
        quantity = BigDecimal.ZERO;
        entryPrice = BigDecimal.ZERO;
        positionType = PositionType.NONE;
    }

    public TradePnl calculateTradePnl(PositionType side, BigDecimal entry, BigDecimal exit, BigDecimal qty) {
        if (side == null || entry == null || exit == null || qty == null) {
            throw new IllegalArgumentException("Trade parameters are required");
        }
        BigDecimal priceDiff = switch (side) {
            case LONG -> exit.subtract(entry);
            case SHORT -> entry.subtract(exit);
            default -> BigDecimal.ZERO;
        };
        BigDecimal pnl = qty.multiply(priceDiff);
        BigDecimal entryValue = qty.multiply(entry);
        BigDecimal pnlPct = entryValue.signum() == 0
                ? BigDecimal.ZERO
                : pnl.divide(entryValue, DB_MATH_CONTEXT).multiply(HUNDRED);
        return new TradePnl(pnl, pnlPct);
    }

    private void ensurePositivePrice(BigDecimal price) {
        if (price.signum() <= 0) {
            throw new IllegalStateException("Price must be positive: " + price);
        }
    }

    private void ensureNonNegativeEquity(BigDecimal equity) {
        if (equity.signum() < 0) {
            throw new IllegalStateException("Negative equity: " + equity);
        }
    }

    public record TradePnl(BigDecimal pnl, BigDecimal pnlPct) {
    }
}
