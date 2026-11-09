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
