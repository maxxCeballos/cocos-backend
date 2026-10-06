CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255),
    accountnumber VARCHAR(20)
);

CREATE TABLE instruments (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(10),
    name VARCHAR(255),
    type VARCHAR(10)
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    instrumentid BIGINT,
    userid BIGINT,
    size INTEGER,
    price NUMERIC(10, 2),
    type VARCHAR(10),
    side VARCHAR(10),
    status VARCHAR(20),
    datetime TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_orders_instrument FOREIGN KEY (instrumentid) REFERENCES instruments(id),
    CONSTRAINT fk_orders_user FOREIGN KEY (userid) REFERENCES users(id)
);

CREATE TABLE marketdata (
    id BIGSERIAL PRIMARY KEY,
    instrumentid BIGINT,
    high NUMERIC(10, 2),
    low NUMERIC(10, 2),
    open NUMERIC(10, 2),
    close NUMERIC(10, 2),
    previousclose NUMERIC(10, 2),
    date DATE,
    CONSTRAINT fk_marketdata_instrument FOREIGN KEY (instrumentid) REFERENCES instruments(id)
);
