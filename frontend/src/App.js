import React from "react";
import ChartView from "./Chart";

export default function App() {
    return (
        <div style={{padding: 20}}>
            <h2>Trade Data – Candlestick + VWAP + Volume Profile</h2>
            <ChartView/>
        </div>
    );
}
