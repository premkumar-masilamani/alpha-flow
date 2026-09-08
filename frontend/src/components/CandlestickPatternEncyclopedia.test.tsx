import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import CandlestickPatternEncyclopedia from './CandlestickPatternEncyclopedia';
import { CANDLESTICK_PATTERNS } from '../config/candlestickPatterns';

describe('CandlestickPatternEncyclopedia', () => {
    beforeEach(() => {
        // Mock scrollIntoView
        Element.prototype.scrollIntoView = vi.fn();
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.clearAllTimers();
        vi.useRealTimers();
    });

    it('renders all 68 patterns and initial controls by default', () => {
        render(<CandlestickPatternEncyclopedia />);

        expect(screen.getByPlaceholderText(/Search patterns/i)).toBeInTheDocument();
        expect(screen.getByText(`of ${CANDLESTICK_PATTERNS.length} patterns`)).toBeInTheDocument();
        expect(screen.getByRole('tab', { name: /All \(68\)/i })).toBeInTheDocument();
        expect(screen.getByText('Part I: Bullish Reversals')).toBeInTheDocument();
        expect(screen.getByText('Hammer (1)')).toBeInTheDocument();
        expect(screen.getByText('Three White Soldiers (3)')).toBeInTheDocument();
    });

    it('filters patterns based on search query', () => {
        render(<CandlestickPatternEncyclopedia />);

        const searchInput = screen.getByPlaceholderText(/Search patterns/i);
        fireEvent.change(searchInput, { target: { value: 'hanging man' } });

        expect(screen.getByText('Hanging Man (1)')).toBeInTheDocument();
        expect(screen.queryByText('Three White Soldiers (3)')).not.toBeInTheDocument();
    });

    it('clears search input when clear button is clicked', () => {
        render(<CandlestickPatternEncyclopedia />);

        const searchInput = screen.getByPlaceholderText(/Search patterns/i) as HTMLInputElement;
        fireEvent.change(searchInput, { target: { value: 'morning star' } });
        expect(searchInput.value).toBe('morning star');

        const clearBtn = screen.getByLabelText('Clear search');
        fireEvent.click(clearBtn);

        expect(searchInput.value).toBe('');
        expect(screen.getByText('Hammer (1)')).toBeInTheDocument();
    });

    it('filters patterns by category group tab', () => {
        render(<CandlestickPatternEncyclopedia />);

        const contTab = screen.getByRole('tab', { name: /Bullish Continuations/i });
        fireEvent.click(contTab);

        expect(screen.getByText('Part III: Bullish Continuations')).toBeInTheDocument();
        expect(screen.queryByText('Part I: Bullish Reversals')).not.toBeInTheDocument();
        expect(screen.getByText('Rising Three Methods (5)')).toBeInTheDocument();
    });

    it('displays empty state and resets filters properly', () => {
        render(<CandlestickPatternEncyclopedia />);

        const searchInput = screen.getByPlaceholderText(/Search patterns/i) as HTMLInputElement;
        fireEvent.change(searchInput, { target: { value: 'zzzznonexistentpattern' } });

        expect(screen.getByText('No matching patterns found')).toBeInTheDocument();
        expect(screen.queryByText('Hammer (1)')).not.toBeInTheDocument();

        const resetBtn = screen.getByRole('button', { name: /Reset All Filters/i });
        fireEvent.click(resetBtn);

        expect(searchInput.value).toBe('');
        expect(screen.getByText('Hammer (1)')).toBeInTheDocument();
    });

    it('handles initialPatternId and attempts to scroll to the element', () => {
        const { container } = render(<CandlestickPatternEncyclopedia initialPatternId="hammer" />);

        const patternElement = container.querySelector('#pattern-hammer');
        expect(patternElement).toBeInTheDocument();
        expect(patternElement?.className).toContain('ring-2');

        act(() => {
            vi.advanceTimersByTime(200);
        });
        expect(Element.prototype.scrollIntoView).toHaveBeenCalled();

        act(() => {
            vi.advanceTimersByTime(4000);
        });
        expect(patternElement?.className).not.toContain('ring-2');
    });
});
