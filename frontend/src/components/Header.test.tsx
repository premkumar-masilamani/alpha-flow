import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Header from './Header';
import * as api from '../services/api';

vi.mock('../services/api', () => ({
    getCachedMarketHolidays: vi.fn(),
}));

describe('Header Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders the header with the title, calendar, clock, and dual market badges', async () => {
        vi.mocked(api.getCachedMarketHolidays).mockResolvedValueOnce([
            {
                id: 1,
                country: 'US',
                holidayDate: '2026-01-01',
                name: 'New Year’s Day',
            },
        ]);

        render(<Header />);

        const titleElement = screen.getByText('Alpha Flow');
        expect(titleElement).toBeInTheDocument();
        expect(titleElement).toHaveAttribute('id', 'app-title');

        const nseBadge = screen.getByText(/NSE:/i);
        expect(nseBadge).toBeInTheDocument();

        const nyseBadge = screen.getByText(/NYSE:/i);
        expect(nyseBadge).toBeInTheDocument();

        await waitFor(() => {
            expect(api.getCachedMarketHolidays).toHaveBeenCalledTimes(1);
        });
    });

    it('gracefully handles holiday fetch failure', async () => {
        vi.mocked(api.getCachedMarketHolidays).mockRejectedValueOnce(new Error('Network error'));

        render(<Header />);

        const titleElement = screen.getByText('Alpha Flow');
        expect(titleElement).toBeInTheDocument();

        const nseBadge = screen.getByText(/NSE:/i);
        expect(nseBadge).toBeInTheDocument();
    });
});
