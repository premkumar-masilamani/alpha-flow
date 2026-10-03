# Alpha Flow

AlphaFlow studies the flow of buying and selling to make better trading decisions.

## Pre-requisites

- Java version 25 (LTS)
- Node version 26
- Docker & Docker Compose (for local PostgreSQL database)

## Local Dev Setup

On a terminal, run:

```bash
make run_database
make run_backend
```

On another terminal, run:

```bash
make run_frontend
```

## Connect to the database

```bash
make connect_database
```

---

## Angel One SmartAPI (15-Minute Intraday Data)

Alpha Flow uses **Angel One SmartAPI** to fetch real-time market quotes and 15-minute historical candlestick data for Indian tickers (such as `NIFTY 50`).

> **Note:** Without Angel One credentials, the application still runs normally for daily and weekly timeframes (via Yahoo Finance), but the **Intra-day (15m)** tab will display *"No data available for this ticker"*, and the intraday scheduler will remain idle.

### 1. Obtain Angel One Credentials

1. **Register / Log in**: Sign up at the [Angel One SmartAPI Developer Portal](https://smartapi.angelone.in/).
2. **Create an App**: Create a new app (type: Historical API / Trading API) to generate your **API Key**.
3. **Enable TOTP**: Enable Time-based One-Time Password (TOTP) authentication on your Angel One account to obtain your **TOTP Secret Key** (Base32 format, e.g. `JBSWY3DPEHPK3PXP`).
4. **Identify Client Code & MPIN**:
   - **Client Code**: Your Angel One trading account User ID (e.g., `A123456`).
   - **Password**: Your 4-digit Angel One MPIN or trading password.

### 2. Configure Environment Variables

Export the credentials in your local shell environment (e.g. in `~/.zshrc`, `~/.bashrc`, or before running `make run_backend`):

```bash
export ANGEL_ONE_API_KEY="your_api_key_here"
export ANGEL_ONE_CLIENT_CODE="your_client_code_here"
export ANGEL_ONE_PASSWORD="your_mpin_here"
export ANGEL_ONE_TOTP_KEY="your_totp_key_here"
```
