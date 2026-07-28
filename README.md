# xian-nova-portfoliomanager

## Tech stack

- Java 17
- Spring Boot 3.3
- Spring Security
- Spring JDBC
- MySQL 8
- Static frontend: Bootstrap + Vanilla JS + Chart.js

## Database
created `portfolioDb` with `users` and `portfolio_items` and `price_snapshots` tables. 

## Price Snapshot Module

- Purpose: fetch the latest quote by `ticker`, persist it as a snapshot, and support latest-price queries.
- Core flow: Controller receives request -> Service calls external price API -> Repository writes into `price_snapshots` -> API returns a standard response.
- Snapshot fields: `id`, `ticker`, `latestPrice`, `fetchedAt`, `rawPayload` (mapped from `PriceSnapshotResponse`).

### API Endpoints (`/api/price-snapshots`)

- `POST /{ticker}/sync`: sync and persist the latest price; returns `201 Created` with snapshot data.
- `POST /{ticker}/refresh`: refresh and persist the latest price; returns snapshot data.
- `GET /{ticker}`: query the most recently stored price snapshot for the given `ticker`.
- `GET /{ticker}/live`: fetch and persist in real time; returns `502 Bad Gateway` if the external price service fails.

### Quick Usage

1. Call `POST /api/price-snapshots/AAPL/sync` to store a new snapshot.
2. Call `GET /api/price-snapshots/AAPL` to retrieve the latest stored price.
3. The frontend can poll `GET /api/price-snapshots/{ticker}/live` for near real-time updates.

## Four-Person Work Division Plan

| Member | Git Username        | Responsible Module                             |
| ------ | ------------------- | ---------------------------------------------- |
| Yang   | QY                  | Project Architecture & Security Authentication |
| Viggo  | Middle-Earth-source | Portfolio Core Business Logic                  |
| Grace  | wangdi-666          | Price Snapshot Module                          |
| Susan  | scyyw8              | Frontend UI, Testing & Documentation           |