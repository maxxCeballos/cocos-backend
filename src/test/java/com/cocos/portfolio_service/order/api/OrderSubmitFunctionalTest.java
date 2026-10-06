package com.cocos.portfolio_service.order.api;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;
import com.cocos.portfolio_service.instrument.infrastructure.persistence.InstrumentEntity;
import com.cocos.portfolio_service.instrument.infrastructure.persistence.InstrumentJpaRepository;
import com.cocos.portfolio_service.instrument.infrastructure.persistence.InstrumentRepositoryAdapter;
import com.cocos.portfolio_service.instrument.utils.mappers.InstrumentMapper;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.infrastructure.persistence.MarketDataEntity;
import com.cocos.portfolio_service.marketdata.infrastructure.persistence.MarketDataJpaRepository;
import com.cocos.portfolio_service.marketdata.infrastructure.persistence.MarketDataRepositoryAdapter;
import com.cocos.portfolio_service.marketdata.utils.mappers.MarketDataMapper;
import com.cocos.portfolio_service.order.application.OrderService;
import com.cocos.portfolio_service.order.application.OrderServiceTestFactory;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.infrastructure.persistence.OrderEntity;
import com.cocos.portfolio_service.order.infrastructure.persistence.OrderJpaRepository;
import com.cocos.portfolio_service.order.infrastructure.persistence.OrderRepositoryAdapter;
import com.cocos.portfolio_service.order.utils.mappers.OrderMapper;
import com.cocos.portfolio_service.shared.api.GlobalExceptionHandler;
import com.cocos.portfolio_service.shared.infrastructure.lock.SharedLockService;
import com.cocos.portfolio_service.user.domain.UserRepository;
import com.cocos.portfolio_service.user.infrastructure.persistence.UserEntity;
import com.cocos.portfolio_service.user.infrastructure.persistence.UserJpaRepository;
import com.cocos.portfolio_service.user.infrastructure.persistence.UserRepositoryAdapter;
import com.cocos.portfolio_service.user.infrastructure.persistence.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

/** Full HTTP-to-adapter order flow; only Spring Data repositories and Redis are simulated. */
class OrderSubmitFunctionalTest {
    private UserJpaRepository userJpa;
    private InstrumentJpaRepository instrumentJpa;
    private MarketDataJpaRepository marketDataJpa;
    private OrderJpaRepository orderJpa;
    private OrderMapper orderMapper;
    private MockMvc mockMvc;
    private SharedLockService lockService;
    private final List<OrderEntity> baselineOrders = new CopyOnWriteArrayList<>();
    private final List<OrderEntity> savedOrders = new CopyOnWriteArrayList<>();
    private final Map<String, String> redisValues = new ConcurrentHashMap<>();
    private final AtomicInteger lockAttempts = new AtomicInteger();
    private final AtomicInteger saveEntries = new AtomicInteger();
    private final AtomicInteger activeSaves = new AtomicInteger();
    private final AtomicInteger maxActiveSaves = new AtomicInteger();
    private final AtomicBoolean holdFirstSave = new AtomicBoolean();
    private final AtomicReference<CountDownLatch> firstSaveEntered = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> releaseFirstSave = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> secondLockAttempt = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> secondSaveEntered = new AtomicReference<>();

    record OrderCase(String name, OrderSide side, OrderType type, Long instrumentId,
                     String successStatus, Failure failure) { }
    enum Failure { REJECTED, BAD_REQUEST }

    static Stream<OrderCase> orderCases() {
        return Stream.of(
                new OrderCase("buy market", OrderSide.BUY, OrderType.MARKET, 30L, "FILLED", Failure.REJECTED),
                new OrderCase("buy limit", OrderSide.BUY, OrderType.LIMIT, 30L, "NEW", Failure.REJECTED),
                new OrderCase("sell market", OrderSide.SELL, OrderType.MARKET, 30L, "FILLED", Failure.REJECTED),
                new OrderCase("sell limit", OrderSide.SELL, OrderType.LIMIT, 30L, "NEW", Failure.REJECTED),
                new OrderCase("cash in", OrderSide.CASH_IN, OrderType.MARKET, 65L, "FILLED", Failure.BAD_REQUEST),
                new OrderCase("cash out", OrderSide.CASH_OUT, OrderType.MARKET, 65L, "FILLED", Failure.REJECTED));
    }

    @BeforeEach
    void setUp() {
        userJpa = mock(UserJpaRepository.class);
        instrumentJpa = mock(InstrumentJpaRepository.class);
        marketDataJpa = mock(MarketDataJpaRepository.class);
        orderJpa = mock(OrderJpaRepository.class);
        orderMapper = Mappers.getMapper(OrderMapper.class);

        when(userJpa.findById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return id == 404 ? Optional.empty() : Optional.of(userEntity(id));
        });
        when(instrumentJpa.findById(anyLong())).thenAnswer(inv -> Optional.of(instrumentEntity(inv.getArgument(0))));
        when(marketDataJpa.findLatestByInstrumentId(anyLong()))
                .thenAnswer(inv -> Optional.of(marketDataEntity(inv.getArgument(0))));
        when(orderJpa.findByUserIdAndStatusNotIn(anyLong(), anyList())).thenAnswer(inv -> {
            long userId = inv.getArgument(0);
            List<OrderStatus> excluded = inv.getArgument(1);
            return Stream.concat(baselineOrders.stream(), savedOrders.stream())
                    .filter(order -> order.getUserId().equals(userId) && !excluded.contains(order.getStatus()))
                    .toList();
        });
        when(orderJpa.save(any(OrderEntity.class))).thenAnswer(inv -> persist(inv.getArgument(0)));

        configureRedisLock();
        UserRepository users = new UserRepositoryAdapter(userJpa, Mappers.getMapper(UserMapper.class));
        InstrumentRepository instruments = new InstrumentRepositoryAdapter(instrumentJpa, Mappers.getMapper(InstrumentMapper.class));
        MarketDataRepository marketData = new MarketDataRepositoryAdapter(marketDataJpa, Mappers.getMapper(MarketDataMapper.class));
        OrderRepository orders = new OrderRepositoryAdapter(orderJpa, orderMapper);
        OrderService service = OrderServiceTestFactory.create(users, orders, instruments, marketData, lockService);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(service, orderMapper))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        prepareOrders(List.of());
    }

    @AfterEach
    void stopLockRenewer() {
        if (lockService != null) ReflectionTestUtils.invokeMethod(lockService, "shutdownRenewer");
    }

    @ParameterizedTest(name = "{0}: succeeds and then rejects or reports an invalid order")
    @MethodSource("orderCases")
    void submitsEachOrderKindThroughControllerAndAdapters(OrderCase testCase) throws Exception {
        prepareOrders(successBaseline(testCase));
        var success = submit(testCase, testCase.type());
        success.andExpect(status().isCreated())
                .andExpect(jsonPath("$.side").value(testCase.side().name()))
                .andExpect(jsonPath("$.instrumentId").value(testCase.instrumentId()))
                .andExpect(jsonPath("$.status").value(testCase.successStatus()));
        assertEquals(1, savedOrders.size());
        assertEquals(OrderStatus.valueOf(testCase.successStatus()), savedOrders.getFirst().getStatus());

        prepareOrders(failureBaseline(testCase));
        if (testCase.failure() == Failure.BAD_REQUEST) {
            submit(testCase, OrderType.LIMIT)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Request is invalid"));
            assertTrue(savedOrders.isEmpty());
        } else {
            submit(testCase, testCase.type())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
            assertEquals(1, savedOrders.size());
            assertEquals(OrderStatus.REJECTED, savedOrders.getFirst().getStatus());
        }
        verify(orderJpa, atLeastOnce()).save(any(OrderEntity.class));
    }

    @Test
    void returnsNotFoundWhenHeaderReferencesUnknownUser() throws Exception {
        mockMvc.perform(post("/api/orders/submit").header("X-User-Id", "404")
                        .contentType(MediaType.APPLICATION_JSON).content(request(30L, OrderSide.BUY, OrderType.MARKET)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        verify(orderJpa, never()).save(any());
    }

    @RepeatedTest(3)
    void concurrentRequestsForOneUserCannotOverlapInsideOrderPersistence() throws Exception {
        prepareOrders(List.of(cashDeposit(7L, 1000)));
        enableSaveBarrier();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(() -> performStatus(7L));
            assertTrue(firstSaveEntered.get().await(3, TimeUnit.SECONDS), "first save did not enter");
            Future<Integer> second = pool.submit(() -> performStatus(7L));
            assertTrue(secondLockAttempt.get().await(3, TimeUnit.SECONDS), "second request did not wait on the lock");
            assertEquals(1, saveEntries.get(), "second request entered the critical section before release");
            releaseFirstSave.get().countDown();
            assertEquals(201, first.get(3, TimeUnit.SECONDS));
            assertEquals(201, second.get(3, TimeUnit.SECONDS));
            assertEquals(2, saveEntries.get());
            assertEquals(1, maxActiveSaves.get(), "same-user saves overlapped");
        } finally {
            releaseBarrier();
            pool.shutdownNow();
        }
    }

    @RepeatedTest(3)
    void differentUsersCanEnterPersistenceConcurrently() throws Exception {
        prepareOrders(List.of(cashDeposit(7L, 1000), cashDeposit(8L, 1000)));
        enableSaveBarrier();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(() -> performStatus(7L));
            assertTrue(firstSaveEntered.get().await(3, TimeUnit.SECONDS), "first save did not enter");
            Future<Integer> second = pool.submit(() -> performStatus(8L));
            assertTrue(secondSaveEntered.get().await(3, TimeUnit.SECONDS), "different user was blocked by another user's lock");
            assertEquals(2, maxActiveSaves.get(), "different users did not overlap");
            releaseFirstSave.get().countDown();
            assertEquals(201, first.get(3, TimeUnit.SECONDS));
            assertEquals(201, second.get(3, TimeUnit.SECONDS));
        } finally {
            releaseBarrier();
            pool.shutdownNow();
        }
    }

    private org.springframework.test.web.servlet.ResultActions submit(OrderCase testCase, OrderType type) throws Exception {
        return mockMvc.perform(post("/api/orders/submit").header("X-User-Id", "7")
                .contentType(MediaType.APPLICATION_JSON).content(request(testCase.instrumentId(), testCase.side(), type)));
    }

    private int performStatus(long userId) throws Exception {
        return mockMvc.perform(post("/api/orders/submit").header("X-User-Id", Long.toString(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(30L, OrderSide.BUY, OrderType.MARKET)))
                .andReturn().getResponse().getStatus();
    }

    private static String request(Long instrumentId, OrderSide side, OrderType type) {
        return """
                {"instrumentId":%d,"side":"%s","type":"%s","size":1,"price":100,"budget":0}
                """.formatted(instrumentId, side, type).trim();
    }

    private List<OrderEntity> successBaseline(OrderCase testCase) {
        return switch (testCase.side()) {
            case BUY, CASH_OUT -> List.of(cashDeposit(7L, 1000));
            case SELL -> List.of(filledBuy(7L, 30L, 2));
            case CASH_IN -> List.of();
        };
    }

    private List<OrderEntity> failureBaseline(OrderCase testCase) {
        return switch (testCase.side()) {
            case BUY -> List.of(cashDeposit(7L, 10));
            case SELL -> List.of();
            case CASH_OUT -> List.of();
            case CASH_IN -> List.of();
        };
    }

    private void prepareOrders(List<OrderEntity> initial) {
        baselineOrders.clear();
        baselineOrders.addAll(initial);
        savedOrders.clear();
        saveEntries.set(0);
        activeSaves.set(0);
        maxActiveSaves.set(0);
        holdFirstSave.set(false);
        firstSaveEntered.set(null);
        releaseFirstSave.set(null);
        secondLockAttempt.set(null);
        secondSaveEntered.set(null);
        lockAttempts.set(0);
        redisValues.clear();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void configureRedisLock() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String owner = invocation.getArgument(1);
            int attempt = lockAttempts.incrementAndGet();
            CountDownLatch latch = secondLockAttempt.get();
            if (attempt >= 2 && latch != null) latch.countDown();
            return redisValues.putIfAbsent(key, owner) == null;
        });
        configureLockOperationsForCurrentStore(redis);
        lockService = new SharedLockService(redis, Duration.ofSeconds(30), Duration.ofSeconds(20),
                Duration.ofSeconds(5), Duration.ofMillis(2));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void configureLockOperationsForCurrentStore(StringRedisTemplate redis) {
        doAnswer(invocation -> {
            RedisScript<?> script = invocation.getArgument(0);
            @SuppressWarnings("unchecked") List<String> keys = invocation.getArgument(1);
            String key = keys.getFirst();
            String owner = (String) invocation.getArgument(2);
            if (script.getScriptAsString().contains("pexpire")) {
                return owner.equals(redisValues.get(key)) ? 1L : 0L;
            }
            return redisValues.remove(key, owner) ? 1L : 0L;
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    private OrderEntity persist(OrderEntity entity) {
        int entry = saveEntries.incrementAndGet();
        int active = activeSaves.incrementAndGet();
        maxActiveSaves.accumulateAndGet(active, Math::max);
        try {
            CountDownLatch entered = firstSaveEntered.get();
            if (entry == 1 && holdFirstSave.get() && entered != null) {
                entered.countDown();
                if (!releaseFirstSave.get().await(4, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting to release first save");
                }
            }
            CountDownLatch second = secondSaveEntered.get();
            if (entry == 2 && second != null) second.countDown();
            entity.setId((long) entry);
            savedOrders.add(entity);
            return entity;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } finally {
            activeSaves.decrementAndGet();
        }
    }

    private void enableSaveBarrier() {
        holdFirstSave.set(true);
        firstSaveEntered.set(new CountDownLatch(1));
        releaseFirstSave.set(new CountDownLatch(1));
        secondLockAttempt.set(new CountDownLatch(1));
        secondSaveEntered.set(new CountDownLatch(1));
    }

    private void releaseBarrier() {
        CountDownLatch latch = releaseFirstSave.get();
        if (latch != null) latch.countDown();
    }

    private static UserEntity userEntity(long id) {
        UserEntity entity = mock(UserEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getEmail()).thenReturn("user" + id + "@example.com");
        when(entity.getAccountNumber()).thenReturn("account-" + id);
        return entity;
    }

    private static InstrumentEntity instrumentEntity(long id) {
        InstrumentEntity entity = mock(InstrumentEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getTicker()).thenReturn(id == 65 ? "ARS" : "GGAL");
        when(entity.getName()).thenReturn(id == 65 ? "Pesos" : "Grupo Galicia");
        when(entity.getType()).thenReturn(id == 65 ? InstrumentType.MONEDA : InstrumentType.ACCIONES);
        return entity;
    }

    private static MarketDataEntity marketDataEntity(long instrumentId) {
        MarketDataEntity entity = mock(MarketDataEntity.class);
        when(entity.getId()).thenReturn(1L);
        when(entity.getInstrumentId()).thenReturn(instrumentId);
        when(entity.getClose()).thenReturn(new BigDecimal("100.00"));
        when(entity.getPreviousClose()).thenReturn(new BigDecimal("95.00"));
        when(entity.getDate()).thenReturn(LocalDate.of(2026, 10, 1));
        return entity;
    }

    private static OrderEntity cashDeposit(long userId, int amount) {
        OrderEntity entity = new OrderEntity();
        entity.setId(100L + userId);
        entity.setUserId(userId);
        entity.setInstrumentId(65L);
        entity.setSide(OrderSide.CASH_IN);
        entity.setType(OrderType.MARKET);
        entity.setSize(amount);
        entity.setPrice(BigDecimal.ONE);
        entity.setStatus(OrderStatus.FILLED);
        entity.setDatetime(LocalDateTime.of(2026, 10, 1, 10, 0));
        return entity;
    }

    private static OrderEntity filledBuy(long userId, long instrumentId, int size) {
        OrderEntity entity = new OrderEntity();
        entity.setId(200L + userId);
        entity.setUserId(userId);
        entity.setInstrumentId(instrumentId);
        entity.setSide(OrderSide.BUY);
        entity.setType(OrderType.MARKET);
        entity.setSize(size);
        entity.setPrice(new BigDecimal("100.00"));
        entity.setStatus(OrderStatus.FILLED);
        entity.setDatetime(LocalDateTime.of(2026, 10, 1, 10, 0));
        return entity;
    }
}
