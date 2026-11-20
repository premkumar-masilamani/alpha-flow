import type { Time } from 'lightweight-charts';
import type { DailyCandleData, IndicatorSeries } from '../services/api';

export interface SanitizedCandle {
    time: Time;
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    vol: number;
}

export interface SanitizedPoint {
    time: Time;
    value: number;
}

export const toChartTime = (dateStr: string | number): Time => {
    if (typeof dateStr === 'number') return dateStr as Time;
    if (!dateStr) return '' as Time;
    const str = String(dateStr);
    return (str.length >= 10 ? str.substring(0, 10) : str) as Time;
};

export const compareChartTimes = (a: Time, b: Time): number => {
    if (typeof a === 'number' && typeof b === 'number') {
        return a - b;
    }
    return String(a).localeCompare(String(b));
};

export const sanitizeCandleData = (data: DailyCandleData[]): SanitizedCandle[] => {
    if (!Array.isArray(data) || data.length === 0) return [];

    const validItems: SanitizedCandle[] = [];
    for (const d of data) {
        if (
            !d ||
            typeof d.date !== 'string' ||
            !d.date.trim() ||
            d.open == null ||
            d.high == null ||
            d.low == null ||
            d.close == null ||
            d.vol == null
        ) continue;
        const open = Number(d.open);
        const high = Number(d.high);
        const low = Number(d.low);
        const close = Number(d.close);
        const vol = Number(d.vol);
        if (
            Number.isFinite(open) &&
            Number.isFinite(high) &&
            Number.isFinite(low) &&
            Number.isFinite(close) &&
            Number.isFinite(vol)
        ) {
            validItems.push({
                time: toChartTime(d.date),
                date: d.date,
                open,
                high,
                low,
                close,
                vol,
            });
        }
    }

    // Deduplicate by chart time (last entry with the same timestamp wins)
    const dedupedMap = new Map<string | number, SanitizedCandle>();
    for (const item of validItems) {
        dedupedMap.set(item.time as (string | number), item);
    }

    // Sort in strictly ascending time order
    return Array.from(dedupedMap.values()).sort((a, b) => compareChartTimes(a.time, b.time));
};

export const sanitizeIndicatorPoints = (
    series: IndicatorSeries,
    output: string
): SanitizedPoint[] => {
    if (!series || !Array.isArray(series.points) || series.points.length === 0) {
        return [];
    }

    const validPoints: SanitizedPoint[] = [];
    for (const p of series.points) {
        if (!p || typeof p.date !== 'string' || !p.date.trim() || !p.values) continue;
        const raw = p.values[output];
        if (raw === undefined || raw === null) continue;
        const val = Number(raw);
        if (Number.isFinite(val)) {
            validPoints.push({
                time: toChartTime(p.date),
                value: val,
            });
        }
    }

    // Deduplicate by chart time (last entry with the same timestamp wins)
    const dedupedMap = new Map<string | number, SanitizedPoint>();
    for (const point of validPoints) {
        dedupedMap.set(point.time as (string | number), point);
    }

    // Sort in strictly ascending time order
    return Array.from(dedupedMap.values()).sort((a, b) => compareChartTimes(a.time, b.time));
};
