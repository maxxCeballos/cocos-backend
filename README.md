# cocos-backend
cocos-challenge-backend

## Considerations and assumptions

- `CASH_IN` deposits are assumed to be whole values only. The current order model stores `size` as an integer and uses a price of `1`, so it cannot represent fractional deposits such as `1000.5`.

## Portfolio calculations

### Account values

These formulas describe the current API behavior. An order's value is `price × size`; cancelled and rejected orders are ignored.

```text
availableCash = sum(CASH_IN values)
              + sum(FILLED SELL values)
              - sum(CASH_OUT values)
              - sum(FILLED BUY values)

onHoldCash = sum(NEW BUY values)

stockShareValue = sum over instruments(
    (sum(FILLED BUY sizes) - sum(FILLED SELL sizes)) × latest close
)

totalAccountValue = availableCash + onHoldCash + stockShareValue
```

`NEW SELL` orders do not affect available cash, on-hold cash, or stock share value. `NEW BUY` order values are reported in `onHoldCash` and do not affect `stockShareValue`.

### Instrument return calculation

The API's `totalReturnPercent` field measures position performance by accumulating daily profit and loss from market prices and filled trades. For each market quote, the daily market movement is valued against the position size at the start of that quote's date:

```text
dailyMarketPnL = (close - previousClose) × opening position size

BUY adjustment  = (close - execution price) × size
SELL adjustment = (execution price - close) × size
dailyPnL = dailyMarketPnL + sum(BUY and SELL adjustments)

totalPnL = initial position value and pre-history trade cash flows
         + sum(dailyPnL)
purchaseAmount = sum(FILLED BUY price × size)
totalReturnPercent = (totalPnL / purchaseAmount) × 100
```

Orders are matched to the first market quote on or after their date. Orders before the first quote initialize the position and its value from that quote's `previousClose`; orders after the last quote are valued against the last `close`. Only `FILLED` buy and sell orders are included. The percentage is rounded to four decimal places. If there is no valid market history or no filled buy amount, the API returns `0.0000`. The calculation does not include fees, taxes, or dividends.

For example, buying 500 units at 250 and valuing them at the latest close of 229.50 gives a purchase amount of 125,000 and a current market value of 114,750. With no filled sales, the daily P&L accumulation results in `(114,750 - 125,000) / 125,000 × 100 = -8.2000%`.

## Run locally with Docker Compose

Docker Compose starts the Spring Boot API and a PostgreSQL 18.6 database. On first startup, Flyway creates the schema and loads the committed database snapshot.

```bash
docker compose up --build
```

- The API is available at <http://localhost:8080>
- Swagger UI is at <http://localhost:8080/swagger-ui/index.html>. 
- PostgreSQL is published on port `5432` by default. The local defaults are database `portfolio`, username `portfolio`, and password `portfolio`.
- ValkeyCache is published on port `6379`

Override the local defaults with `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, or `APP_PORT` in the environment before starting Compose. These defaults are for local development only.

Stop the containers and keep the database volume with:

```bash
docker compose down
```

To delete the local database and rerun both migrations from scratch, use:

```bash
docker compose down --volumes
docker compose up --build
```

The seed migration contains the selected snapshot from the configured database.
