import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import PriceSummaryBanner from './PriceSummaryBanner';

describe('PriceSummaryBanner Component', () => {
    it('renders prices, positive change with icon, OHLCV, and static date badge', () => {
        render(
            <PriceSummaryBanner
                ltp={2450.5}
                change={25.5}
                changePct={1.05}
                changeSubtitle="(vs open)"
                open={2425.0}
                high={2460.0}
                low={2420.0}
                close={2450.5}
                volume={1500000}
                date="FRI, OCT 16, 2026"
            />
        );

        // LTP
        expect(screen.getByText('Last Traded Price')).toBeInTheDocument();
        expect(screen.getByText('2,450.50')).toBeInTheDocument();

        // Change
        expect(screen.getByText('(vs open)')).toBeInTheDocument();
        expect(screen.getByText('+25.50 (+1.05%)')).toBeInTheDocument();

        // OHLCV
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:2425.00');
        expect(screen.getByText('High:').parentElement).toHaveTextContent('High:2460.00');
        expect(screen.getByText('Low:').parentElement).toHaveTextContent('Low:2420.00');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:2450.50');
        expect(screen.getByText('Vol:').parentElement).toHaveTextContent('Vol:1,500,000');

        // Static date badge
        expect(screen.getByText('FRI, OCT 16, 2026')).toBeInTheDocument();
    });

    it('renders negative change with minus sign and negative percentage', () => {
        render(
            <PriceSummaryBanner
                ltp={1980.25}
                change={-20.75}
                changePct={-1.04}
                open={2001.0}
                high={2005.0}
                low={1975.0}
                close={1980.25}
            />
        );

        expect(screen.getByText('-20.75 (-1.04%)')).toBeInTheDocument();
    });

    it('handles null / empty state gracefully with dashes', () => {
        render(
            <PriceSummaryBanner
                ltp={null}
                change={null}
                changePct={null}
                open={null}
                high={null}
                low={null}
                close={null}
                date="MON, OCT 19, 2026"
            />
        );

        expect(screen.getByText('Last Traded Price').parentElement).toHaveTextContent('Last Traded Price—');
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:—');
        expect(screen.getByText('High:').parentElement).toHaveTextContent('High:—');
        expect(screen.getByText('Low:').parentElement).toHaveTextContent('Low:—');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:—');
        expect(screen.queryByText('Vol:')).not.toBeInTheDocument();
        expect(screen.getByText('MON, OCT 19, 2026')).toBeInTheDocument();
    });
});
