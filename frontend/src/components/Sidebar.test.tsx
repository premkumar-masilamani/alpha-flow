import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Sidebar from './Sidebar';
import type { Ticker } from '../services/api';

describe('Sidebar Component', () => {
    const mockTickers: Ticker[] = [
        { id: 1, symbol: '^GSPC', name: 'S&P 500', type: 'INDEX', country: 'US' },
        { id: 2, symbol: '^NSEI', name: 'NIFTY 50', type: 'INDEX', country: 'IN' },
        { id: 3, symbol: 'AAPL', name: 'Apple Inc.', type: 'STOCK', country: 'US' },
        { id: 4, symbol: 'MSFT', name: 'Microsoft Corporation', type: 'STOCK', country: 'US' },
        { id: 5, symbol: 'TSLA', name: 'Tesla Inc.' }
    ];

    it('renders sections with "country - ticker_type" headers, indices first followed by stocks', () => {
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker={null}
                onSelectTicker={vi.fn()}
            />
        );

        const inIndexHeading = screen.getByText('IN - INDEX');
        const usIndexHeading = screen.getByText('US - INDEX');
        const usStockHeading = screen.getByText('US - STOCK');

        expect(inIndexHeading).toBeInTheDocument();
        expect(usIndexHeading).toBeInTheDocument();
        expect(usStockHeading).toBeInTheDocument();

        // Verify order: INDEX sections appear before STOCK sections in DOM
        const headings = screen.getAllByRole('button', { name: /(IN|US) - (INDEX|STOCK)/i });
        expect(headings[0]).toHaveTextContent('IN - INDEX');
        expect(headings[1]).toHaveTextContent('US - INDEX');
        expect(headings[2]).toHaveTextContent('US - STOCK');

        // Verify all tickers are rendered
        mockTickers.forEach((ticker) => {
            expect(screen.getByText(ticker.symbol)).toBeInTheDocument();
            expect(screen.getByText(ticker.name)).toBeInTheDocument();
        });

        // Verify count badges
        expect(screen.getAllByText('1')).toHaveLength(2); // IN - INDEX (1) and US - INDEX (1)
        expect(screen.getByText('3')).toBeInTheDocument(); // US - STOCK (3 tickers)
    });

    it('collapses and expands sections when header is clicked', () => {
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker={null}
                onSelectTicker={vi.fn()}
            />
        );

        // Initially expanded
        expect(screen.getByText('^NSEI')).toBeInTheDocument();

        // Click IN - INDEX header button to collapse
        const inIndexButton = screen.getByRole('button', { name: /IN - INDEX/i });
        fireEvent.click(inIndexButton);

        // NIFTY 50 should now be hidden
        expect(screen.queryByText('^NSEI')).not.toBeInTheDocument();

        // Other sections should still be visible
        expect(screen.getByText('^GSPC')).toBeInTheDocument();
        expect(screen.getByText('AAPL')).toBeInTheDocument();

        // Click again to expand
        fireEvent.click(inIndexButton);
        expect(screen.getByText('^NSEI')).toBeInTheDocument();
    });

    it('applies selected classes to the active ticker', () => {
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker="AAPL"
                onSelectTicker={vi.fn()}
            />
        );

        const aaplButton = screen.getByText('AAPL').closest('button');
        const msftButton = screen.getByText('MSFT').closest('button');

        expect(aaplButton).toHaveClass('bg-slate-700', 'text-white', 'border-l-4', 'border-blue-500');
        expect(msftButton).not.toHaveClass('bg-slate-700', 'text-white', 'border-l-4', 'border-blue-500');
        expect(msftButton).toHaveClass('pl-5');
    });

    it('calls onSelectTicker when an index or stock ticker is clicked', () => {
        const handleSelectTicker = vi.fn();
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker={null}
                onSelectTicker={handleSelectTicker}
            />
        );

        const nseiButton = screen.getByText('^NSEI').closest('button');
        expect(nseiButton).toBeInTheDocument();
        fireEvent.click(nseiButton!);
        expect(handleSelectTicker).toHaveBeenCalledWith('^NSEI');

        const aaplButton = screen.getByText('AAPL').closest('button');
        expect(aaplButton).toBeInTheDocument();
        fireEvent.click(aaplButton!);
        expect(handleSelectTicker).toHaveBeenCalledWith('AAPL');
    });
});
