import { describe, it, expect } from 'vitest';
import { getMarketStatus } from './marketStatus';

describe('getMarketStatus', () => {
    it('identifies weekend status on Saturday and Sunday', () => {
        // Saturday 2026-10-10 12:00 UTC
        const saturday = new Date('2026-10-10T12:00:00Z');
        const usStatus = getMarketStatus(saturday, 'US');
        expect(usStatus.isOpen).toBe(false);
        expect(usStatus.statusType).toBe('WEEKEND');
        expect(usStatus.displayMessage).toBe('Closed • Weekend');

        // Sunday 2026-10-11 12:00 UTC
        const sunday = new Date('2026-10-11T12:00:00Z');
        const inStatus = getMarketStatus(sunday, 'IN');
        expect(inStatus.isOpen).toBe(false);
        expect(inStatus.statusType).toBe('WEEKEND');
        expect(inStatus.displayMessage).toBe('Closed • Weekend');
    });

    it('identifies open session for India NSE during regular trading hours', () => {
        // Wednesday 2026-10-07 at 10:30 IST (05:00 UTC)
        const wednesdayMorning = new Date('2026-10-07T05:00:00Z');
        const inStatus = getMarketStatus(wednesdayMorning, 'IN');
        expect(inStatus.isOpen).toBe(true);
        expect(inStatus.statusType).toBe('OPEN');
        expect(inStatus.displayMessage).toBe('Open (09:15 - 15:30 IST)');
    });

    it('identifies open session for US NYSE during regular trading hours', () => {
        // Wednesday 2026-10-07 at 14:00 EDT (18:00 UTC in EDT)
        const wednesdayAfternoon = new Date('2026-10-07T18:00:00Z');
        const usStatus = getMarketStatus(wednesdayAfternoon, 'US');
        expect(usStatus.isOpen).toBe(true);
        expect(usStatus.statusType).toBe('OPEN');
        expect(usStatus.displayMessage).toBe('Open (09:30 - 16:00 EST)');
    });

    it('identifies pre-market status before session open', () => {
        // Wednesday 2026-10-07 at 08:30 IST (03:00 UTC)
        const earlyIndia = new Date('2026-10-07T03:00:00Z');
        const inStatus = getMarketStatus(earlyIndia, 'IN');
        expect(inStatus.isOpen).toBe(false);
        expect(inStatus.statusType).toBe('PRE_MARKET');
        expect(inStatus.displayMessage).toBe('Closed (Opens 09:15 IST)');

        // Wednesday 2026-10-07 at 08:00 EDT (12:00 UTC)
        const earlyUs = new Date('2026-10-07T12:00:00Z');
        const usStatus = getMarketStatus(earlyUs, 'US');
        expect(usStatus.isOpen).toBe(false);
        expect(usStatus.statusType).toBe('PRE_MARKET');
        expect(usStatus.displayMessage).toBe('Closed (Opens 09:30 EST)');
    });

    it('identifies after-hours status after session close', () => {
        // Wednesday 2026-10-07 at 17:00 IST (11:30 UTC)
        const lateIndia = new Date('2026-10-07T11:30:00Z');
        const inStatus = getMarketStatus(lateIndia, 'IN');
        expect(inStatus.isOpen).toBe(false);
        expect(inStatus.statusType).toBe('AFTER_HOURS');
        expect(inStatus.displayMessage).toBe('Closed (Opens 09:15 IST)');

        // Wednesday 2026-10-07 at 18:00 EDT (22:00 UTC)
        const lateUs = new Date('2026-10-07T22:00:00Z');
        const usStatus = getMarketStatus(lateUs, 'US');
        expect(usStatus.isOpen).toBe(false);
        expect(usStatus.statusType).toBe('AFTER_HOURS');
        expect(usStatus.displayMessage).toBe('Closed (Opens 09:30 EST)');
    });
});
