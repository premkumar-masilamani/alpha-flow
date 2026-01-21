import React, { useEffect, useRef, useState } from "react";
import { createChart } from "lightweight-charts";
import Papa from "papaparse";

/* ---------- Helpers ---------- */

function toBusinessDay(raw) {
    if (raw == null) return null;
    const dateStr = String(raw).replace(/"/g, "").trim();
    const parts = dateStr.split("-");
    if (parts.length !== 3) return null;

    const y = Number(parts[0]);
    const m = Number(parts[1]);
    const d = Number(parts[2]);

    if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) return null;
    return { year: y, month: m, day: d };
}

const isValidNumber = v => Number.isFinite(v);

/* ---------- Component ---------- */

export default function ChartView() {
    const containerRef = useRef(null);
    const legendRef = useRef(null);
    const seriesRef = useRef({});

    // ---- Toggle state ----
    const [showCandles, setShowCandles] = useState(true);
    const [showVWAP, setShowVWAP] = useState(true);
    const [showPOC, setShowPOC] = useState(true);
    const [showVAH, setShowVAH] = useState(true);
    const [showVAL, setShowVAL] = useState(true);

    /* ---------- Chart init ---------- */

    useEffect(() => {
        const chart = createChart(containerRef.current, {
            width: containerRef.current.clientWidth || 900,
            height: 500,
            layout: {
                backgroundColor: "#0f172a",
                textColor: "#cbd5f5",
            },
            grid: {
                vertLines: { color: "#1e293b" },
                horzLines: { color: "#1e293b" },
            },
            rightPriceScale: { autoScale: true },
            timeScale: { timeVisible: true },
        });

        const priceFormat = {
            type: "price",
            precision: 8,
            minMove: 0.00000001,
        };

        // ---- Series ----
        const candles = chart.addCandlestickSeries({
            priceFormat,
            upColor: "#22c55e",
            downColor: "#ef4444",
            borderUpColor: "#22c55e",
            borderDownColor: "#ef4444",
            wickUpColor: "#22c55e",
            wickDownColor: "#ef4444",
        });

        const vwap = chart.addLineSeries({ color: "#eab308", lineWidth: 2, priceFormat });
        const poc  = chart.addLineSeries({ color: "#38bdf8", lineWidth: 2, priceFormat });
        const vah  = chart.addLineSeries({ color: "#a78bfa", lineWidth: 2, priceFormat });
        const val  = chart.addLineSeries({ color: "#f472b6", lineWidth: 2, priceFormat });

        seriesRef.current = { chart, candles, vwap, poc, vah, val };

        /* ---------- CSV ---------- */

        Papa.parse("/trade_data.csv", {
            download: true,
            header: true,
            dynamicTyping: true,
            skipEmptyLines: true,
            transformHeader: h => h.replace(/"/g, "").trim(),
            complete: (result) => {
                const rows = result.data;

                candles.setData(
                    rows
                        .map(r => {
                            const t = toBusinessDay(r.trade_date);
                            const o = Number(r.price_open);
                            const h = Number(r.price_high);
                            const l = Number(r.price_low);
                            const c = Number(r.price_close);
                            return t && isValidNumber(o) && isValidNumber(h) && isValidNumber(l) && isValidNumber(c)
                                ? { time: t, open: o, high: h, low: l, close: c }
                                : null;
                        })
                        .filter(Boolean)
                );

                const mapLine = field =>
                    rows
                        .map(r => {
                            const t = toBusinessDay(r.trade_date);
                            const v = Number(r[field]);
                            return t && isValidNumber(v) ? { time: t, value: v } : null;
                        })
                        .filter(Boolean);

                vwap.setData(mapLine("vwap"));
                poc.setData(mapLine("volume_profile_poc"));
                vah.setData(mapLine("volume_profile_vah"));
                val.setData(mapLine("volume_profile_val"));

                chart.timeScale().fitContent();
            },
        });

        /* ---------- Legend ---------- */

        chart.subscribeCrosshairMove(param => {
            if (!param.time || !legendRef.current) return;

            const prices = param.seriesPrices;
            const c = prices.get(candles);

            legendRef.current.innerHTML = `
        ${showCandles && c ? `<b>OHLC</b>: ${c.open} / ${c.high} / ${c.low} / ${c.close}<br/>` : ""}
        ${showVWAP ? `<span style="color:#eab308">VWAP</span>: ${prices.get(vwap) ?? "-"}<br/>` : ""}
        ${showPOC  ? `<span style="color:#38bdf8">POC</span>: ${prices.get(poc) ?? "-"}<br/>` : ""}
        ${showVAH  ? `<span style="color:#a78bfa">VAH</span>: ${prices.get(vah) ?? "-"}<br/>` : ""}
        ${showVAL  ? `<span style="color:#f472b6">VAL</span>: ${prices.get(val) ?? "-"}` : ""}
      `;
        });

        return () => chart.remove();
    }, []);

    /* ---------- Toggle effects ---------- */

    useEffect(() => {
        const { candles, vwap, poc, vah, val } = seriesRef.current;
        if (!candles) return;

        candles.applyOptions({ visible: showCandles });
        vwap.applyOptions({ visible: showVWAP });
        poc.applyOptions({ visible: showPOC });
        vah.applyOptions({ visible: showVAH });
        val.applyOptions({ visible: showVAL });
    }, [showCandles, showVWAP, showPOC, showVAH, showVAL]);

    /* ---------- UI ---------- */

    return (
        <>
            <div style={{ marginBottom: 8 }}>
                <label>
                    <input type="checkbox" checked={showCandles} onChange={e => setShowCandles(e.target.checked)} /> Candles
                </label>{" "}
                <label>
                    <input type="checkbox" checked={showVWAP} onChange={e => setShowVWAP(e.target.checked)} /> VWAP
                </label>{" "}
                <label>
                    <input type="checkbox" checked={showPOC} onChange={e => setShowPOC(e.target.checked)} /> POC
                </label>{" "}
                <label>
                    <input type="checkbox" checked={showVAH} onChange={e => setShowVAH(e.target.checked)} /> VAH
                </label>{" "}
                <label>
                    <input type="checkbox" checked={showVAL} onChange={e => setShowVAL(e.target.checked)} /> VAL
                </label>
            </div>

            <div style={{ position: "relative" }}>
                <div
                    ref={legendRef}
                    style={{
                        position: "absolute",
                        top: 8,
                        left: 8,
                        zIndex: 10,
                        background: "rgba(15,23,42,0.85)",
                        color: "#e5e7eb",
                        padding: "8px 10px",
                        fontSize: "12px",
                        borderRadius: "4px",
                        pointerEvents: "none",
                    }}
                />
                <div ref={containerRef} style={{ width: "100%", height: "500px" }} />
            </div>
        </>
    );
}
