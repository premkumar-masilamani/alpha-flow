import type { MarketHoliday } from '../services/api';

export type MarketCountry = 'IN' | 'US';
export type MarketStatusType = 'OPEN' | 'HOLIDAY' | 'WEEKEND' | 'PRE_MARKET' | 'AFTER_HOURS';

export interface MarketStatus {
    country: MarketCountry;
    exchange: 'NSE' | 'NYSE';
    isOpen: boolean;
    statusType: MarketStatusType;
    holidayName: string | null;
    displayMessage: string;
}

interface MarketConfig {
    timeZone: string;
    exchange: 'NSE' | 'NYSE';
    startHour: number;
    startMinute: number;
    endHour: number;
    endMinute: number;
    tzAbbreviation: string;
}

const MARKET_CONFIGS: Record<MarketCountry, MarketConfig> = {
    IN: {
        timeZone: 'Asia/Kolkata',
        exchange: 'NSE',
        startHour: 9,
        startMinute: 15,
        endHour: 15,
        endMinute: 30,
        tzAbbreviation: 'IST',
    },
    US: {
        timeZone: 'America/New_York',
        exchange: 'NYSE',
        startHour: 9,
        startMinute: 30,
        endHour: 16,
        endMinute: 0,
        tzAbbreviation: 'EST',
    },
};

export const getMarketParts = (date: Date, timeZone: string) => {
    const formatter = new Intl.DateTimeFormat('en-US', {
        timeZone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        weekday: 'short',
        hourCycle: 'h23',
    });
    const parts = formatter.formatToParts(date);
    const getPart = (type: string) => parts.find((p) => p.type === type)?.value || '';

    const year = getPart('year');
    const month = getPart('month');
    const day = getPart('day');
    const hour = parseInt(getPart('hour'), 10);
    const minute = parseInt(getPart('minute'), 10);
    const weekday = getPart('weekday');
    const dateKey = `${year}-${month}-${day}`;

    return { year, month, day, hour, minute, weekday, dateKey };
};

export const getMarketStatus = (
    now: Date,
    country: MarketCountry,
    holidays: MarketHoliday[]
): MarketStatus => {
    const config = MARKET_CONFIGS[country];
    const { hour, minute, weekday, dateKey } = getMarketParts(now, config.timeZone);

    const padTime = (h: number, m: number) =>
        `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;

    const sessionHoursStr = `${padTime(config.startHour, config.startMinute)} - ${padTime(config.endHour, config.endMinute)} ${config.tzAbbreviation}`;
    const openTimeStr = `${padTime(config.startHour, config.startMinute)} ${config.tzAbbreviation}`;

    // 1. Weekend check
    if (weekday === 'Sat' || weekday === 'Sun') {
        return {
            country,
            exchange: config.exchange,
            isOpen: false,
            statusType: 'WEEKEND',
            holidayName: null,
            displayMessage: 'Closed • Weekend',
        };
    }

    // 2. Holiday check
    const holidayMatch = holidays.find(
        (h) => h.country === country && h.holidayDate === dateKey
    );
    if (holidayMatch) {
        return {
            country,
            exchange: config.exchange,
            isOpen: false,
            statusType: 'HOLIDAY',
            holidayName: holidayMatch.name,
            displayMessage: `Closed • ${holidayMatch.name}`,
        };
    }

    // 3. Regular trading day session hours check
    const currentMinutes = hour * 60 + minute;
    const startMinutes = config.startHour * 60 + config.startMinute;
    const endMinutes = config.endHour * 60 + config.endMinute;

    if (currentMinutes >= startMinutes && currentMinutes < endMinutes) {
        return {
            country,
            exchange: config.exchange,
            isOpen: true,
            statusType: 'OPEN',
            holidayName: null,
            displayMessage: `Open (${sessionHoursStr})`,
        };
    }

    if (currentMinutes < startMinutes) {
        return {
            country,
            exchange: config.exchange,
            isOpen: false,
            statusType: 'PRE_MARKET',
            holidayName: null,
            displayMessage: `Closed (Opens ${openTimeStr})`,
        };
    }

    return {
        country,
        exchange: config.exchange,
        isOpen: false,
        statusType: 'AFTER_HOURS',
        holidayName: null,
        displayMessage: `Closed (Opens ${openTimeStr})`,
    };
};
