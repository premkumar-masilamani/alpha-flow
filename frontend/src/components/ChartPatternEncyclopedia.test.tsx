import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import ChartPatternEncyclopedia from './ChartPatternEncyclopedia';
import { CHART_PATTERNS } from '../config/chartPatterns';

describe('ChartPatternEncyclopedia', () => {
    beforeEach(() => {
        Element.prototype.scrollIntoView = vi.fn();
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.clearAllTimers();
        vi.useRealTimers();
    });

    it('renders all 12 patterns and initial controls by default', () => {
        render(<ChartPatternEncyclopedia />);

        expect(screen.getByPlaceholderText(/Search chart patterns/i)).toBeInTheDocument();
        expect(screen.getByText(`of ${CHART_PATTERNS.length} patterns`)).toBeInTheDocument();
        expect(screen.getByRole('tab', { name: /All \(12\)/i })).toBeInTheDocument();
        expect(screen.getByText('Part I: Bullish Reversals')).toBeInTheDocument();
        expect(screen.getByText('Double Top')).toBeInTheDocument();
        expect(screen.getByText('Double Bottom')).toBeInTheDocument();
        expect(screen.getByText('Cup and Handle')).toBeInTheDocument();
    });

    it('filters patterns based on search query', () => {
        render(<ChartPatternEncyclopedia />);

        const searchInput = screen.getByPlaceholderText(/Search chart patterns/i);
        fireEvent.change(searchInput, { target: { value: 'head and shoulders' } });

        expect(screen.getByText('Head and Shoulders')).toBeInTheDocument();
        expect(screen.getByText('Inverse Head and Shoulders')).toBeInTheDocument();
        expect(screen.queryByText('Double Top')).not.toBeInTheDocument();
    });

    it('clears search input when clear button is clicked', () => {
        render(<ChartPatternEncyclopedia />);

        const searchInput = screen.getByPlaceholderText(/Search chart patterns/i) as HTMLInputElement;
        fireEvent.change(searchInput, { target: { value: 'wedge' } });
        expect(searchInput.value).toBe('wedge');

        const clearBtn = screen.getByLabelText('Clear search');
        fireEvent.click(clearBtn);

        expect(searchInput.value).toBe('');
        expect(screen.getByText('Double Top')).toBeInTheDocument();
    });

    it('filters patterns by category group tab', () => {
        render(<ChartPatternEncyclopedia />);

        const contTab = screen.getByRole('tab', { name: /Bullish Continuations/i });
        fireEvent.click(contTab);

        expect(screen.getByText('Part III: Bullish Continuations')).toBeInTheDocument();
        expect(screen.queryByText('Part I: Bullish Reversals')).not.toBeInTheDocument();
        expect(screen.getByText('Cup and Handle')).toBeInTheDocument();
        expect(screen.getByText('Ascending Triangle')).toBeInTheDocument();
        expect(screen.getByText('Symmetrical Triangle')).toBeInTheDocument();
    });

    it('displays empty state and resets filters properly', () => {
        render(<ChartPatternEncyclopedia />);

        const searchInput = screen.getByPlaceholderText(/Search chart patterns/i) as HTMLInputElement;
        fireEvent.change(searchInput, { target: { value: 'zzzznonexistentchartpattern' } });

        expect(screen.getByText('No matching chart patterns found')).toBeInTheDocument();
        expect(screen.queryByText('Double Top')).not.toBeInTheDocument();

        const resetBtn = screen.getByRole('button', { name: /Reset All Filters/i });
        fireEvent.click(resetBtn);

        expect(searchInput.value).toBe('');
        expect(screen.getByText('Double Top')).toBeInTheDocument();
    });

    it('handles initialPatternId and attempts to scroll to the element', () => {
        const { container } = render(<ChartPatternEncyclopedia initialPatternId="double-bottom" />);

        const patternElement = container.querySelector('#chart-pattern-double-bottom');
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
