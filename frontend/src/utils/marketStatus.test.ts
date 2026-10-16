import { describe, it, expect } from 'vitest';
import { getMarketStatus, getExpectedIntradayDateKey } from './marketStatus';
import type { MarketHoliday } from '../services/api';

describe('getMarketStatus', () => {
    const mockHolidays: MarketHoliday[] = [
        {
            id: 1,
            country: 'US',
            holidayDate: '2026-01-19',
            name: 'Martin Luther King, Jr. Day',
        },
        {
            id: 2,
            country: 'IN',
            holidayDate: '2026-11-24',
            name: 'Prakash Gurpurb Sri Guru Nanak Dev',
        },
    ];

    it('identifies weekend status on Saturday and Sunday', () => {
        // Saturday 2026-10-10 12:00 UTC
        const saturday = new Date('2026-10-10T12:00:00Z');
        const usStatus = getMarketStatus(saturday, 'US', mockHolidays);
        expect(usStatus.isOpen).toBe(false);
        expect(usStatus.statusType).toBe('WEEKEND');
        expect(usStatus.displayMessage).toBe('Closed • Weekend');

        // Sunday 2026-10-11 12:00 UTC
        const sunday = new Date('2026-10-11T12:00:00Z');
        const inStatus = getMarketStatus(sunday, 'IN', mockHolidays);
        expect(inStatus.isOpen).toBe(false);
        expect(inStatus.statusType).toBe('WEEKEND');
        expect(inStatus.displayMessage).toBe('Closed • Weekend');
    });

    it('identifies holidays with full names without truncation', () => {
        // US MLK Day 2026-01-19 (15:00 UTC = 10:00 EST)
        const mlkDay = new Date('2026-01-19T15:00:00Z');
        const usStatus = getMarketStatus(mlkDay, 'US', mockHolidays);
        expect(usStatus.isOpen).toBe(false);
        expect(usStatus.statusType).toBe('HOLIDAY');
        expect(usStatus.holidayName).toBe('Martin Luther King, Jr. Day');
        expect(usStatus.displayMessage).toBe('Closed • Martin Luther King, Jr. Day');

        // India Guru Nanak Jayanti 2026-11-24 (05:00 UTC = 10:30 IST)
        const guruNanakDay = new Date('2026-11-24T05:00:00Z');
        const inStatus = getMarketStatus(guruNanakDay, 'IN', mockHolidays);
        expect(inStatus.isOpen).toBe(false);
        expect(inStatus.statusType).toBe('HOLIDAY');
        expect(inStatus.holidayName).toBe('Prakash Gurpurb Sri Guru Nanak Dev');
        expect(inStatus.displayMessage).toBe('Closed • Prakash Gurpurb Sri Guru Nanak Dev');
    });

    it('identifies open session for India NSE during regular trading hours', () => {
        // Wednesday 2026-10-07 at 10:30 IST (05:00 UTC)
        const wednesdayMorning = new Date('2026-10-07T05:00:00Z');
        const inStatus = getMarketStatus(wednesdayMorning, 'IN', mockHolidays);
        expect(inStatus.isOpen).toBe(true);
        expect(inStatus.statusType).toBe('OPEN');
        expect(inStatus.displayMessage).toBe('Open (09:15 - 15:30 IST)');
    });

    it('identifies open session for US NYSE during regular trading hours', () => {
        // Wednesday 2026-10-07 at 14:00 EDT (18:00 UTC in EDT)
        const wednesdayAfternoon = new Date('2026-10-07T18:00:00Z');
        const usStatus = getMarketStatus(wednesdayAfternoon, 'US', mockHolidays);
        expect(usStatus.isOpen).toBe(true);
        expect(usStatus.statusType).toBe('OPEN');
        expect(usStatus.displayMessage).toBe('Open (09:30 - 16:00 EST)');
    });

    it('identifies pre-market status before session open', () => {
        // Wednesday 2026-10-07 at 08:30 IST (03:00 UTC)
        const earlyIndia = new Date('2026-10-07T03:00:00Z');
        const inStatus = getMarketStatus(earlyIndia, 'IN', mockHolidays);
        expect(inStatus.isOpen).toBe(false);
        expect(inStatus.statusType).toBe('PRE_MARKET');
        expect(inStatus.displayMessage).toBe('Closed (Opens 09:15 IST)');

        // Wednesday 2026-10-07 at 08:00 EDT (12:00 UTC)
        const earlyUs = new Date('2026-10-07T12:00:00Z');
        const usStatus = getMarketStatus(earlyUs, 'US', mockHolidays);
        expect(usStatus.isOpen).toBe(false);
        expect(usStatus.statusType).toBe('PRE_MARKET');
        expect(usStatus.displayMessage).toBe('Closed (Opens 09:30 EST)');
    });

    it('identifies after-hours status after session close', () => {
        // Wednesday 2026-10-07 at 17:00 IST (11:30 UTC)
        const lateIndia = new Date('2026-10-07T11:30:00Z');
        const inStatus = getMarketStatus(lateIndia, 'IN', mockHolidays);
        expect(inStatus.isOpen).toBe(false);
        expect(inStatus.statusType).toBe('AFTER_HOURS');
        expect(inStatus.displayMessage).toBe('Closed (Opens 09:15 IST)');

        // Wednesday 2026-10-07 at 18:00 EDT (22:00 UTC)
        const lateUs = new Date('2026-10-07T22:00:00Z');
        const usStatus = getMarketStatus(lateUs, 'US', mockHolidays);
        expect(usStatus.isOpen).toBe(false);
        expect(usStatus.statusType).toBe('AFTER_HOURS');
        expect(usStatus.displayMessage).toBe('Closed (Opens 09:30 EST)');
    });
});

describe('getExpectedIntradayDateKey', () => {
    const mockHolidays: MarketHoliday[] = [
        {
            id: 1,
            country: 'US',
            holidayDate: '2026-01-19',
            name: 'Martin Luther King, Jr. Day',
        },
        {
            id: 2,
            country: 'IN',
            holidayDate: '2026-11-24',
            name: 'Prakash Gurpurb Sri Guru Nanak Dev',
        },
    ];

    it('returns the market dateKey when the market is OPEN on a trading day', () => {
        // Friday 2026-10-16 at 09:31 IST (04:01 UTC)
        const fridayOpenIn = new Date('2026-10-16T04:01:00Z');
        expect(getExpectedIntradayDateKey(fridayOpenIn, 'IN', mockHolidays)).toBe('2026-10-16');

        // Friday 2026-10-16 at 10:00 EDT (14:00 UTC)
        const fridayOpenUs = new Date('2026-10-16T14:00:00Z');
        expect(getExpectedIntradayDateKey(fridayOpenUs, 'US', mockHolidays)).toBe('2026-10-16');
    });

    it('returns the market dateKey during AFTER_HOURS on a trading day', () => {
        // Friday 2026-10-16 at 16:30 IST (11:00 UTC)
        const fridayAfterHoursIn = new Date('2026-10-16T11:00:00Z');
        expect(getExpectedIntradayDateKey(fridayAfterHoursIn, 'IN', mockHolidays)).toBe('2026-10-16');

        // Friday 2026-10-16 at 17:00 EDT (21:00 UTC)
        const fridayAfterHoursUs = new Date('2026-10-16T21:00:00Z');
        expect(getExpectedIntradayDateKey(fridayAfterHoursUs, 'US', mockHolidays)).toBe('2026-10-16');
    });

    it('returns null during PRE_MARKET, WEEKEND, and HOLIDAY states', () => {
        // Pre-market: Friday 2026-10-16 at 08:30 IST (03:00 UTC)
        const fridayPreMarketIn = new Date('2026-10-16T03:00:00Z');
        expect(getExpectedIntradayDateKey(fridayPreMarketIn, 'IN', mockHolidays)).toBeNull();

        // Weekend: Saturday 2026-10-17 at 10:00 IST (04:30 UTC)
        const saturdayIn = new Date('2026-10-17T04:30:00Z');
        expect(getExpectedIntradayDateKey(saturdayIn, 'IN', mockHolidays)).toBeNull();

        // Holiday: Tuesday 2026-11-24 at 10:30 IST (05:00 UTC)
        const holidayIn = new Date('2026-11-24T05:00:00Z');
        expect(getExpectedIntradayDateKey(holidayIn, 'IN', mockHolidays)).toBeNull();
    });
});

