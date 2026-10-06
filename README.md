# cocos-backend
cocos-challenge-backend

## Consideraciones y supuestos

- Agregué un docker compose para ejecutar todo en docker el servicio, base de datos y caché. 
- La base de datos que me pasaron, hice un seed para migrarlo desde la ejecución del servicio, no es necesasrio ponerle las credenciales que me pasaron.
- Utilicé un cache Valkey para guardar las busquedas de instrumentos por userId, para hacer el lock al momento de crear una orden. Esto es para indicar que hay una sección crítica que se debe ejecutar en exclusión mutua. En el Test funcional de ordenes se ejecutan threads en paralelo para validar esta funcionalidad.
- Respecto a las reglas de negocio:
  - Asumo que los depósitos `CASH_IN` son únicamente valores enteros. El modelo actual de órdenes almacena `size` como un entero y utiliza un precio de `1`, por lo que no puede representar depósitos fraccionarios como `1000.5`.
  - Agregué un value object Money para representar el concepto de Moneda. En el proyecto por defecto todo esta calculado en base ARS, agregue USD como placeholder para indicar que podria calcularse tambien en dolares.
  - Para crear una orden, el field `budget` es para indicar un monto en lugar de la cantidad de acciones a comprar, y hace el calculo de la cantidad de acciones en base al precio de la accion y el presupuesto definido por el usuario. Este field aplica unicamente para compras, para venta se me hacía que no aplica porque te puede vender muchas acciones a un precio que quizas no es el que espera el usuario.
  - Agregué un campo label en la respuesta del portfolio para los fields que representan dinero para mostrarlos con el simbolo adecuado, ademas de retornar `currency: AR$`
  - Las formulas que componen el portfolio las describo a continuación.


## Cálculos del portafolio

### Valores de la cuenta

Estas fórmulas describen el comportamiento actual de la API. El valor de una orden es `price × size`; se ignoran las órdenes canceladas y rechazadas.

```text
availableCash = sum(CASH_IN values)
              + sum(FILLED SELL values)
              - sum(NEW BUY values)
              - sum(CASH_OUT values)
              - sum(FILLED BUY values)

onHoldCash = sum(NEW BUY values)

stockShareValue = sum over instruments(
    (sum(FILLED BUY sizes) - sum(FILLED SELL sizes)) × latest close
)

totalAccountValue = availableCash + onHoldCash + stockShareValue
```

Las órdenes `NEW SELL` no afectan `availableCash`, `onHoldCash` ni `stockShareValue`. Los valores de las órdenes `NEW BUY` reducen `availableCash` porque reservan efectivo, se reportan en `onHoldCash` y no afectan `stockShareValue`. Por lo tanto, sumar `availableCash` y `onHoldCash` cuenta el efectivo reservado una sola vez en `totalAccountValue`.

### Cálculo del rendimiento por instrumento

El campo `totalReturnPercent` de la API mide el rendimiento de una posición mediante la acumulación de ganancias y pérdidas diarias a partir de los precios de mercado y las operaciones ejecutadas. Para cada cotización de mercado, el movimiento diario se calcula usando el tamaño de la posición al inicio de la fecha de esa cotización:

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

Las órdenes se asocian con la primera cotización de mercado en su fecha o después de ella. Las órdenes anteriores a la primera cotización inicializan la posición y su valor con `previousClose` de esa cotización; las órdenes posteriores a la última cotización se valoran con el último `close`. Solo se incluyen órdenes de compra y venta `FILLED`. El porcentaje se redondea a cuatro decimales. Si no hay un historial de mercado válido o no hay un monto de compra ejecutada, la API devuelve `0.0000`. El cálculo no incluye comisiones, impuestos ni dividendos.

Por ejemplo, comprar 500 unidades a 250 y valorarlas al último cierre de 229.50 da un monto de compra de 125,000 y un valor de mercado actual de 114,750. Sin ventas ejecutadas, la acumulación diaria de ganancias y pérdidas da como resultado `(114,750 - 125,000) / 125,000 × 100 = -8.2000%`.

## Ejecutar localmente con Docker Compose

Docker Compose inicia la API de Spring Boot y una base de datos PostgreSQL 18.6. En el primer inicio, Flyway crea el esquema y carga el snapshot de la base de datos incluida en el repositorio.

```bash
docker compose up --build
```

- La API está disponible en <http://localhost:8080>
- Swagger UI está en <http://localhost:8080/swagger-ui/index.html>.
- PostgreSQL se publica en el puerto `5432` de forma predeterminada. Los valores locales predeterminados son la base de datos `portfolio`, el usuario `portfolio` y la contraseña `portfolio`.
- ValkeyCache se publica en el puerto `6379`.

Para cambiar los valores locales predeterminados, define `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` o `APP_PORT` en el entorno antes de iniciar Compose. Estos valores predeterminados son solo para desarrollo local.

Detén los contenedores y conserva el volumen de la base de datos con:

```bash
docker compose down
```

Para eliminar la base de datos local y volver a ejecutar ambas migraciones desde cero, usa:

```bash
docker compose down --volumes
docker compose up --build
```

La migración de datos inicial contiene los datos proporcionados en la base de datos configurada.
