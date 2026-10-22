import { describe, it, expect } from 'vitest';
import {
    getMarketStatus,
    getExpectedIntradayDateKey,
    getCalendarDayRange,
    getDayNonTradingReason,
} from './marketStatus';
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
    it('returns the current market dateKey for IN and US markets across any time or day', () => {
        // Friday 2026-10-16 at 09:31 IST (04:01 UTC)
        const fridayOpenIn = new Date('2026-10-16T04:01:00Z');
        expect(getExpectedIntradayDateKey(fridayOpenIn, 'IN')).toBe('2026-10-16');

        // Friday 2026-10-16 at 10:00 EDT (14:00 UTC)
        const fridayOpenUs = new Date('2026-10-16T14:00:00Z');
        expect(getExpectedIntradayDateKey(fridayOpenUs, 'US')).toBe('2026-10-16');

        // Saturday 2026-10-17 at 08:10 IST (02:40 UTC)
        const saturdayMorningIn = new Date('2026-10-17T02:40:00Z');
        expect(getExpectedIntradayDateKey(saturdayMorningIn, 'IN')).toBe('2026-10-17');
    });
});

describe('getCalendarDayRange', () => {
    it('generates every consecutive calendar day from startDateKey to endDateKey inclusive', () => {
        expect(getCalendarDayRange('2026-10-15', '2026-10-18')).toEqual([
            '2026-10-15',
            '2026-10-16',
            '2026-10-17',
            '2026-10-18',
        ]);
    });

    it('returns empty array when start is after end or malformed', () => {
        expect(getCalendarDayRange('2026-10-18', '2026-10-15')).toEqual([]);
        expect(getCalendarDayRange('invalid', '2026-10-15')).toEqual([]);
    });
});

describe('getDayNonTradingReason', () => {
    const mockHolidays: MarketHoliday[] = [
        {
            id: 1,
            country: 'IN',
            holidayDate: '2026-11-24',
            name: 'Prakash Gurpurb Sri Guru Nanak Dev',
        },
    ];

    it('identifies Saturday and Sunday as WEEKEND', () => {
        expect(getDayNonTradingReason('2026-10-17', 'IN', mockHolidays)).toEqual({
            type: 'WEEKEND',
            label: 'Saturday',
        });
        expect(getDayNonTradingReason('2026-10-18', 'IN', mockHolidays)).toEqual({
            type: 'WEEKEND',
            label: 'Sunday',
        });
    });

    it('identifies market holidays with the specific holiday name', () => {
        expect(getDayNonTradingReason('2026-11-24', 'IN', mockHolidays)).toEqual({
            type: 'HOLIDAY',
            label: 'Prakash Gurpurb Sri Guru Nanak Dev',
        });
    });

    it('returns null on regular trading weekdays', () => {
        expect(getDayNonTradingReason('2026-10-16', 'IN', mockHolidays)).toBeNull();
    });
});
