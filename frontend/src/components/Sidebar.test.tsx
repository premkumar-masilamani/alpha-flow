import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Sidebar from './Sidebar';
import type { Ticker } from '../services/api';

describe('Sidebar Component', () => {
    const mockTickers: Ticker[] = [
        { id: 1, symbol: 'AAPL', name: 'Apple Inc.' },
        { id: 2, symbol: 'MSFT', name: 'Microsoft Corporation' },
        { id: 3, symbol: 'TSLA', name: 'Tesla Inc.' }
    ];

    it('renders the list of tickers correctly', () => {
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker={null}
                onSelectTicker={vi.fn()}
            />
        );

        // Check if all ticker symbols are rendered
        mockTickers.forEach((ticker) => {
            expect(screen.getByText(ticker.symbol)).toBeInTheDocument();
            expect(screen.getByText(ticker.name)).toBeInTheDocument();
        });
    });

    it('applies selected classes to the active ticker', () => {
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker="MSFT"
                onSelectTicker={vi.fn()}
            />
        );

        const msftButton = screen.getByText('MSFT').closest('button');
        const aaplButton = screen.getByText('AAPL').closest('button');

        expect(msftButton).toHaveClass('bg-slate-700', 'text-white', 'border-l-4', 'border-blue-500');
        expect(aaplButton).not.toHaveClass('bg-slate-700', 'text-white', 'border-l-4', 'border-blue-500');
        expect(aaplButton).toHaveClass('pl-5');
    });

    it('calls onSelectTicker when a ticker is clicked', () => {
        const handleSelectTicker = vi.fn();
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker={null}
                onSelectTicker={handleSelectTicker}
            />
        );

        const tslaButton = screen.getByText('TSLA').closest('button');
        expect(tslaButton).toBeInTheDocument();

        fireEvent.click(tslaButton!);
        expect(handleSelectTicker).toHaveBeenCalledTimes(1);
        expect(handleSelectTicker).toHaveBeenCalledWith('TSLA');
    });
});
