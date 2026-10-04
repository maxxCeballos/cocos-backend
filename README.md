# cocos-backend
cocos-challenge-backend

## Portfolio calculations

### Account values

These formulas describe the current API behavior. An order's value is `price × size`; cancelled and rejected orders are ignored.

```text
availableCash = sum(CASH_IN values)
              + sum(FILLED SELL values)
              + sum(NEW BUY values)
              - sum(CASH_OUT values)
              - sum(FILLED BUY values)

stockShareValue = sum over instruments(
    (sum(FILLED BUY sizes) - sum(FILLED SELL sizes)) × latest close
)

totalAccountValue = availableCash + stockShareValue
```

`NEW SELL` orders do not affect either available cash or stock share value. In the current implementation, `NEW BUY` order values are added to `availableCash` and do not affect `stockShareValue`.

### Instrument return calculation

The API's `totalReturnPercent` field measures the return on an instrument position using filled buy and sell orders and the instrument's latest market close:

```text
position size = sum(filled BUY sizes) - sum(filled SELL sizes)
purchaseAmount = sum(filled BUY price × size)
saleProceeds = sum(filled SELL price × size)
currentMarketValue = position size × latest close
profitAndLoss = currentMarketValue + saleProceeds - purchaseAmount
totalReturnPercent = (profitAndLoss / purchaseAmount) × 100
```

The percentage is rounded to four decimal places. If there is no market history, no latest close, or no purchase amount, the API returns `0.0000`. The calculation does not include fees, taxes, or dividends.

For example, buying 500 units at 250 and valuing them at the latest close of 229.50 gives a purchase amount of 125,000 and a current market value of 114,750. With no filled sales, the resulting return is `(114,750 - 125,000) / 125,000 × 100 = -8.2000%`.

## Run locally with Docker Compose

Docker Compose starts the Spring Boot API and a PostgreSQL 18.6 database. On first startup, Flyway creates the schema and loads the committed database snapshot.

```bash
docker compose up --build
```

The API is available at <http://localhost:8080>; Swagger UI is at <http://localhost:8080/swagger-ui/index.html>. PostgreSQL is published on port `5432` by default. The local defaults are database `portfolio`, username `portfolio`, and password `portfolio`.

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
