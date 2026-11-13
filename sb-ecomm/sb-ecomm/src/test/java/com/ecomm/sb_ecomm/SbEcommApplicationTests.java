package com.ecomm.sb_ecomm;

import com.ecomm.sb_ecomm.address.model.Address;
import com.ecomm.sb_ecomm.address.repository.AddressRepository;
import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.auth.repository.UserRepository;
import com.ecomm.sb_ecomm.auth.services.UserDetailsImpl;
import com.ecomm.sb_ecomm.cart.model.Cart;
import com.ecomm.sb_ecomm.cart.model.CartItem;
import com.ecomm.sb_ecomm.cart.repository.CartRepository;
import com.ecomm.sb_ecomm.category.model.Category;
import com.ecomm.sb_ecomm.category.repository.CategoryRepository;
import com.ecomm.sb_ecomm.exceptions.newexceptions.ApiException;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.order.payload.OrderDto;
import com.ecomm.sb_ecomm.order.repository.OrderRepository;
import com.ecomm.sb_ecomm.order.services.OrderService;
import com.ecomm.sb_ecomm.payment.model.Payment;
import com.ecomm.sb_ecomm.payment.model.PaymentStatus;
import com.ecomm.sb_ecomm.payment.payload.PaymentDto;
import com.ecomm.sb_ecomm.payment.repository.PaymentRepository;
import com.ecomm.sb_ecomm.payment.services.PaymentService;
import com.ecomm.sb_ecomm.payment.webhook.SignatureUtil;
import com.ecomm.sb_ecomm.payment.webhook.WebhookPayload;
import com.ecomm.sb_ecomm.payment.webhook.WebhookRepository;
import com.ecomm.sb_ecomm.payment.webhook.WebhookService;
import com.ecomm.sb_ecomm.product.model.Product;
import com.ecomm.sb_ecomm.product.repository.ProductRepository;
import com.ecomm.sb_ecomm.validators.OrderExpiryJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DEFINED_PORT: MockGatewayController fires its webhook over real HTTP to localhost:${server.port}.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@Testcontainers
class SbEcommApplicationTests {

    @Container
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.3"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
        r.add("mock.gateway.enabled", () -> "true");
        try (ServerSocket s = new ServerSocket(0)) {
            int port = s.getLocalPort();
            r.add("server.port", () -> port);
        }
    }

    @Value("${mock.gateway.signing-secret}") String signingSecret;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentService paymentService;
    @Autowired CartRepository cartRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired OrderService orderService;
    @Autowired OrderExpiryJob orderExpiryJob;
    @Autowired WebhookRepository webhookRepository;
    @Autowired WebhookService webhookService;

    Long productId;
    Users user;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE order_item, orders, payment, webhook_events, cart_item, cart, addresses, " +
                "products, categories, user_roles, users RESTART IDENTITY CASCADE");

        Category category = new Category();
        category.setCategoryName("Testing Category");
        categoryRepository.save(category);

        Product product = new Product();
        product.setProductName("Test Product");
        product.setDescription("Order/payment workflow test product");
        product.setImage("default.png");
        product.setPrice(100.0);
        product.setDiscount(0.0);
        product.setSpecialPrice(100.0);
        product.setStockQuantity(100);
        product.setReservedQuantity(0);
        product.setCategory(category);
        productId = productRepository.save(product).getId();

        user = userRepository.save(new Users("tester", "password123", "tester@example.com"));

        Address address = new Address("Street 1", "Cairo", "Cairo", "115110", "Egypt");
        address.setUsers(user);
        address.setIsDefault(true);
        addressRepository.save(address);

        fillCart(5);
        loginAs(user);
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    private void loginAs(Users u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UserDetailsImpl.build(u), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /** Puts `quantity` units straight into the cart (bypasses the cart's own stock check). */
    private void fillCart(int quantity) {
        Cart cart = Optional.ofNullable(cartRepository.findByUserEmail(user.getEmail())).orElseGet(() -> {
            Cart c = new Cart();
            c.setUniqueUser(user);
            return c;
        });
        CartItem item = new CartItem();
        item.setProduct(productRepository.findById(productId).orElseThrow());
        item.setQuantity(quantity);
        item.setProductPrice(100.0);
        item.setCart(cart);
        cart.getCartItems().add(item);
        cart.setTotalPrice(100.0 * quantity);
        cartRepository.save(cart);
    }

    private Product product() {
        return productRepository.findById(productId).orElseThrow();
    }

    private Orders order(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    private PaymentStatus paymentStatus(Long id) {
        return paymentRepository.findById(id).orElseThrow().getPaymentStatus();
    }

    private int cartSize() {
        return jdbc.queryForObject("select count(*) from cart_item", Integer.class);
    }

    /** Builds and signs a webhook body exactly like the mock gateway does. */
    private byte[] webhookBody(String eventId, String type, String reference, long timestamp) throws Exception {
        return objectMapper.writeValueAsBytes(new WebhookPayload(eventId, type, reference, timestamp));
    }

    private void deliver(byte[] body) {
        webhookService.handle(body, SignatureUtil.hmacSha256(body, signingSecret));
    }

    private void deliver(String eventId, String type, String reference) throws Exception {
        deliver(webhookBody(eventId, type, reference, System.currentTimeMillis() / 1000));
    }

    private OrderDto placeOrder() {
        return orderService.placeOrder(UUID.randomUUID().toString());
    }

    private PaymentDto cardCheckout(Long orderId) {
        return paymentService.initiatePayment(orderId, "CREDIT CARD");
    }

    // ---------- placing orders ----------

    @Test
    void placeOrder_reservesStock_andEmptiesCart() {
        OrderDto dto = placeOrder();

        assertEquals("PENDING", dto.getOrderStatus());
        assertEquals(500.0, dto.getTotalAmount());
        assertEquals(100, product().getStockQuantity(), "stock only moves when the order is paid");
        assertEquals(5, product().getReservedQuantity(), "the 5 ordered units are reserved");
        assertEquals(0, cartSize(), "the cart became the order, so it's empty");
    }

    @Test
    void placeOrder_insufficientStock_leavesNoOrder() {
        Product p = product();
        p.setStockQuantity(3);
        productRepository.save(p);

        assertThrows(ApiException.class, this::placeOrder);

        assertEquals(0, orderRepository.count(), "a failed order must not be saved");
        assertEquals(0, product().getReservedQuantity(), "nothing reserved");
        assertEquals(1, cartSize(), "the cart is kept so the user can fix it");
    }

    @Test
    void sameKeyTwice_createsOneOrder_reservesOnce() throws Exception {
        String sharedKey = UUID.randomUUID().toString();
        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(threads));
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    orderService.placeOrder(sharedKey);
                } catch (Throwable e) {
                    errors.add(e);
                }
                return null;
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(List.of(), errors, "the losing request returns the winner's order, it doesn't fail");
        assertEquals(1, orderRepository.count(), "same idempotency key must create exactly one order");
        assertEquals(5, product().getReservedQuantity(), "reserved 0 -> 5 once, not 0 -> 10");
    }

    // ---------- payments ----------

    @Test
    void mockGatewayCaptured_endToEnd_confirmsOrder_andDecrementsStockOnce() throws Exception {
        Long orderId = placeOrder().getId();
        PaymentDto payment = cardCheckout(orderId);

        for (int i = 0; i < 2; i++) {   // the second capture (new event id) must be a no-op
            mvc.perform(post("/api/mock-gateway/complete/" + payment.getPgPaymentId())
                            .param("outcome", "CAPTURED")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }

        assertEquals(PaymentStatus.CAPTURED, paymentStatus(payment.getId()));
        assertEquals(OrderStatus.CONFIRMED, order(orderId).getOrderStatus());
        assertEquals(95, product().getStockQuantity(), "stock drops by 5 exactly once");
        assertEquals(0, product().getReservedQuantity());
    }

    @Test
    void duplicateWebhook_isIgnored() throws Exception {
        Long orderId = placeOrder().getId();
        PaymentDto payment = cardCheckout(orderId);
        byte[] body = webhookBody("evt-1", WebhookPayload.CAPTURE, payment.getPgPaymentId(), System.currentTimeMillis() / 1000);

        deliver(body);
        deliver(body);

        assertEquals(95, product().getStockQuantity());
        assertEquals(1, webhookRepository.count(), "one row per event id");
    }

    @Test
    void failedWebhook_marksPaymentFailed_orderStaysPending() throws Exception {
        Long orderId = placeOrder().getId();
        PaymentDto payment = cardCheckout(orderId);

        deliver("evt-fail", WebhookPayload.FAILED, payment.getPgPaymentId());

        assertEquals(PaymentStatus.FAILED, paymentStatus(payment.getId()));
        assertEquals(OrderStatus.PENDING, order(orderId).getOrderStatus(), "the user may retry until the order expires");
        assertEquals(5, product().getReservedQuantity(), "the reservation is kept for the retry");
        assertNotEquals(payment.getId(), cardCheckout(orderId).getId(), "a retry opens a new payment");
    }

    @Test
    void webhookThatFailsProcessing_isReprocessedOnRetry() throws Exception {
        Long orderId = placeOrder().getId();
        PaymentDto payment = cardCheckout(orderId);
        // The gateway knows a reference our DB doesn't have yet (e.g. the webhook beat our commit).
        byte[] body = webhookBody("evt-early", WebhookPayload.CAPTURE, "mock_not_yet_saved", System.currentTimeMillis() / 1000);

        assertThrows(ApiException.class, () -> deliver(body));
        assertEquals(0, webhookRepository.count(), "a failed delivery must not stay claimed");

        Payment p = paymentRepository.findById(payment.getId()).orElseThrow();
        p.setPgPaymentId("mock_not_yet_saved");
        paymentRepository.save(p);

        deliver(body);   // the gateway's retry, same event id

        assertEquals(PaymentStatus.CAPTURED, paymentStatus(payment.getId()));
        assertEquals(OrderStatus.CONFIRMED, order(orderId).getOrderStatus());
    }

    @Test
    void tamperedSignature_isRejected() throws Exception {
        Long orderId = placeOrder().getId();
        PaymentDto payment = cardCheckout(orderId);
        byte[] body = webhookBody("evt-x", WebhookPayload.CAPTURE, payment.getPgPaymentId(), System.currentTimeMillis() / 1000);

        assertThrows(ApiException.class, () -> webhookService.handle(body, "deadbeef"));
        assertEquals(PaymentStatus.PENDING, paymentStatus(payment.getId()));
    }

    @Test
    void staleWebhook_isRejected() throws Exception {
        Long orderId = placeOrder().getId();
        PaymentDto payment = cardCheckout(orderId);
        byte[] body = webhookBody("evt-old", WebhookPayload.CAPTURE, payment.getPgPaymentId(), System.currentTimeMillis() / 1000 - 3600);

        assertThrows(ApiException.class, () -> deliver(body));
        assertEquals(PaymentStatus.PENDING, paymentStatus(payment.getId()));
    }

    @Test
    void doubleCheckout_returnsSamePayment() {
        Long orderId = placeOrder().getId();

        PaymentDto first = cardCheckout(orderId);
        PaymentDto second = cardCheckout(orderId);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, paymentRepository.count());
    }

    @Test
    void invalidPaymentMethod_returns400_andSavesNothing() throws Exception {
        Long orderId = placeOrder().getId();

        mvc.perform(post("/api/user/payments/checkout/" + orderId)
                        .param("paymentMethod", "bitcoin")
                        .with(user("tester").roles("USER")))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void cashOnDelivery_confirmsOrder_andIsNotExpired() {
        Long orderId = placeOrder().getId();

        paymentService.initiatePayment(orderId, "CASH ON DELIVERY");

        assertEquals(OrderStatus.CONFIRMED, order(orderId).getOrderStatus());
        assertEquals(95, product().getStockQuantity());
        assertEquals(0, product().getReservedQuantity());

        Orders o = order(orderId);
        o.setExpirationDate(LocalDateTime.now().minusMinutes(1));
        orderRepository.save(o);
        orderExpiryJob.releaseExpiredReservations();

        assertEquals(OrderStatus.CONFIRMED, order(orderId).getOrderStatus());
        assertEquals(0, product().getReservedQuantity(), "the job must not release stock again");
    }

    @Test
    void captureAfterExpiry_leavesStockAlone() throws Exception {
        Long orderId = placeOrder().getId();
        PaymentDto payment = cardCheckout(orderId);
        Orders o = order(orderId);
        o.setExpirationDate(LocalDateTime.now().minusMinutes(1));
        orderRepository.save(o);
        orderExpiryJob.releaseExpiredReservations();

        deliver("evt-late", WebhookPayload.CAPTURE, payment.getPgPaymentId());

        assertEquals(PaymentStatus.CAPTURED, paymentStatus(payment.getId()), "the money was taken: record it");
        assertEquals(OrderStatus.EXPIRED, order(orderId).getOrderStatus(), "flagged for refund in the log, not revived");
        assertEquals(100, product().getStockQuantity());
        assertEquals(0, product().getReservedQuantity());
    }

    @Test
    void twoConcurrentCaptures_decrementStockOnce() throws Exception {
        Long orderId = placeOrder().getId();
        Long paymentId = cardCheckout(orderId).getId();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    paymentService.updatePaymentStatus(paymentId, PaymentStatus.CAPTURED);
                } catch (Exception ignored) {
                    // the loser may throw; the DB state is what matters
                }
                return null;
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(PaymentStatus.CAPTURED, paymentStatus(paymentId));
        assertEquals(OrderStatus.CONFIRMED, order(orderId).getOrderStatus());
        assertEquals(95, product().getStockQuantity());
        assertEquals(0, product().getReservedQuantity());
    }
}
