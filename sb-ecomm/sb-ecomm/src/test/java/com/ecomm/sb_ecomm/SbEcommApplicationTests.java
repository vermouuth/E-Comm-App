package com.ecomm.sb_ecomm;

import com.ecomm.sb_ecomm.address.model.Address;
import com.ecomm.sb_ecomm.address.repository.AddressRepository;
import com.ecomm.sb_ecomm.auth.model.Users;
import com.ecomm.sb_ecomm.auth.repository.UserRepository;
import com.ecomm.sb_ecomm.cart.repository.CartRepository;
import com.ecomm.sb_ecomm.category.model.Category;
import com.ecomm.sb_ecomm.category.repository.CategoryRepository;
import com.ecomm.sb_ecomm.order.model.OrderItem;
import com.ecomm.sb_ecomm.order.model.Orders;
import com.ecomm.sb_ecomm.order.model.OrderStatus;
import com.ecomm.sb_ecomm.order.payload.OrderDto;
import com.ecomm.sb_ecomm.order.repository.OrderRepository;
import com.ecomm.sb_ecomm.payment.model.Payment;
import com.ecomm.sb_ecomm.payment.model.PaymentMethod;
import com.ecomm.sb_ecomm.payment.model.PaymentStatus;
import com.ecomm.sb_ecomm.payment.payload.PaymentDto;
import com.ecomm.sb_ecomm.payment.repository.PaymentRepository;
import com.ecomm.sb_ecomm.payment.webhook.*;
import com.ecomm.sb_ecomm.product.model.Product;
import com.ecomm.sb_ecomm.product.repository.ProductRepository;
import com.ecomm.sb_ecomm.auth.services.UserDetailsImpl;
import com.ecomm.sb_ecomm.order.services.OrderService;
import com.ecomm.sb_ecomm.payment.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class SbEcommApplicationTests {

    static final DockerImageName PG = DockerImageName.parse("postgres:18.3");

    @Container
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>(PG);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
    }

    @Value("${mock.gateway.signing-secret}")
    String signingSecret;


    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PaymentService paymentService;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    AddressRepository addressRepository;
    @Autowired
    OrderService orderService;
    @Autowired
    WebhookRepository  webhookRepository;
    @Autowired
    WebhookRepository webhookEventRepository;
    @Autowired
    WebhookService webhookService;
    @Autowired
    ObjectMapper objectMapper;

    Long productId;
    Long orderId;
    Long paymentId;
    Users seededUser;
    /*
        @BeforeEach
       void seedForPayment(){
           // 1. category (name must be >= 5 chars)
           Category category = new Category();
           category.setCategoryName("Testing Category");
           categoryRepository.save(category);

           // 2. product: stock = 100, reserved = 5
           Product product = new Product();
           product.setProductName("Test Product");
           product.setDescription("A product used only in the concurrency test");
           product.setImage("default.png");
           product.setPrice(100.0);
           product.setDiscount(0.0);
           product.setSpecialPrice(100.0);
           product.setStockQuantity(100);
           product.setReservedQuantity(5);
           product.setCategory(category);
           product = productRepository.save(product);
           productId = product.getId();

           // 3. user (owns the order + payment)
           Users user = new Users("tester", "password123", "tester@example.com");
           userRepository.save(user);


           Address address = new Address("Street 1", "Cairo", "Cairo", "11511", "Egypt");
           address.setUsers(user);
           address.setIsDefault(true);
           addressRepository.save(address);

           CartItem cartItem = new CartItem();
           cartItem.setProduct(product);
           cartItem.setQuantity(5);
           cartItem.setProductPrice(product.getPrice());
           cartItem.setDiscount(product.getDiscount());

           Cart cart = new Cart();
           cart.setUniqueUser(user);
           cart.setCartItems(List.of(cartItem));
           cart.setTotalPrice(cartItem.getProductPrice() * cartItem.getQuantity());


           // 4. one order item: quantity = 5, pointing at the product
           OrderItem item = new OrderItem();
           item.setProduct(product);
           item.setQuantity(5);
           item.setProductPrice(100.0);
           item.setDiscount(0.0);

           // 5. order: holds the item, belongs to the user
           Orders order = new Orders();
           order.setEmail(user.getEmail());
           order.setUser(user);
           order.setOrderStatus(enOrderStatus.PENDING);
           order.setTotalAmount(500.0);                     // 5 * 100
           order.setExpirationDate(LocalDateTime.now().plusMinutes(15)); // NOT NULL column
           order.setIdempotencyKey(UUID.randomUUID().toString());        // unique column
           order.setStreet("Street 1");
           order.setCity("Cairo");
           order.setState("Cairo");
           order.setZip("11511");
           order.setCountry("Egypt");

           // link both sides of order <-> item, then cascade saves the item
           item.setOrders(order);
           order.setOrderItems(List.of(item));

           // 6. payment: status = AUTHORIZED, linked to the order
           Payment payment = new Payment();
           payment.setUser(user);
           payment.setPaymentMethod(enPaymentMethod.CreditCard);
           payment.setPaymentStatus(enPaymentStatus.AUTHORIZED);
           payment = paymentRepository.save(payment);

           // Orders owns the FK (orders.payment_id), so set it here, then save the order
           order.setPayment(payment);
           orderRepository.save(order);   // cascade persists the OrderItem too

           paymentId = payment.getId();


       }
         */

    /* @BeforeEach
    void seedForOrder(){
        // 1. category (name must be >= 5 chars)
        Category category = new Category();
        category.setCategoryName("Testing Category");
        categoryRepository.save(category);

        // 2. product: stock = 100, reserved = 5
        Product product = new Product();
        product.setProductName("Test Product");
        product.setDescription("A product used only in the concurrency test");
        product.setImage("default.png");
        product.setPrice(100.0);
        product.setDiscount(0.0);
        product.setSpecialPrice(100.0);
        product.setStockQuantity(100);
        product.setReservedQuantity(5);
        product.setCategory(category);
        product = productRepository.save(product);
        productId = product.getId();



        // 3. user (owns the order + payment)
        Users user = new Users("tester", "password123", "tester@example.com");
        userRepository.save(user);

        Address address = new Address("Street 1", "Cairo", "Cairo", "115161", "Egypt");
        address.setUsers(user);
        address.setIsDefault(true);
        addressRepository.save(address);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(5);
        cartItem.setProductPrice(product.getPrice());
        cartItem.setDiscount(product.getDiscount());

        Cart cart = new Cart();
        cart.setUniqueUser(user);
        cart.setTotalPrice(cartItem.getProductPrice() * cartItem.getQuantity());


        cart.setCartItems(List.of(cartItem));
        cartItem.setCart(cart);

        this.cartRepository.save(cart);

        this.seededUser = user;
    }
    */


    @BeforeEach
    void seedForPaymentGatewayTest() {
        Category category = new Category();
        category.setCategoryName("Testing Category");
        categoryRepository.save(category);

        Product product = new Product();
        product.setProductName("Test Product");
        product.setDescription("Webhook capture test product");
        product.setImage("default.png");
        product.setPrice(100.0);
        product.setDiscount(0.0);
        product.setSpecialPrice(100.0);
        product.setStockQuantity(100);
        product.setReservedQuantity(5);   // 5 already reserved by this order
        product.setCategory(category);
        product = productRepository.save(product);
        productId = product.getId();

        Users user = new Users("tester", "password123", "tester@example.com");
        userRepository.save(user);
        seededUser = user;

        Address address = new Address("Street 1", "Cairo", "Cairo", "115110", "Egypt");
        address.setUsers(user);
        address.setIsDefault(true);
        addressRepository.save(address);

        // an order already PENDING with a payment INITIATED at the gateway
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(5);
        item.setProductPrice(100.0);
        item.setDiscount(0.0);

        Orders order = new Orders();
        order.setEmail(user.getEmail());
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalAmount(500.0);
        order.setExpirationDate(LocalDateTime.now().plusMinutes(15));
        order.setIdempotencyKey(UUID.randomUUID().toString());
        order.setStreet("Street 1");
        order.setCity("Cairo");
        order.setState("Cairo");
        order.setZip("115110");
        order.setCountry("Egypt");
        item.setOrders(order);
        order.setOrderItems(List.of(item));

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setPaymentMethod(PaymentMethod.CreditCard);
        payment.setPaymentStatus(PaymentStatus.PENDING);   // waiting on the webhook
        payment.setAmount(BigDecimal.valueOf(500.00));
        payment.setCurrency("EGP");
        payment.setPgPaymentId("mock_ref_123");              // gateway reference
        payment = paymentRepository.save(payment);
        paymentId = payment.getId();

        order.setPayment(payment);
        orderRepository.save(order);
        orderId = order.getId();

        // auth context, in case any downstream call reads it
        var auth = new UsernamePasswordAuthenticationToken(
               UserDetailsImpl.build(user),
                null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // helper: build a signed webhook body exactly like the mock gateway does
    private byte[] signedBody(WebhookPayload payload, String[] sigOut) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(payload);
        sigOut[0] = SignatureUtil.hmacSha256(body, signingSecret);
        return body;
    }

    @Test
    void capturedWebhook_marksPaymentCaptured_andDecrementsStock() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID().toString(),
                "CAPTURE",
                "mock_ref_123",
                System.currentTimeMillis() / 1000);

        String[] sig = new String[1];
        byte[] body = signedBody(payload, sig);

        webhookService.handle(body, sig[0]);

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        assertEquals(PaymentStatus.CAPTURED, payment.getPaymentStatus(),
                "payment should be CAPTURED after the captured webhook");
        assertEquals(95, product.getStockQuantity(),
                "stock should drop by 5 (100 -> 95) on capture");
        assertEquals(0, product.getReservedQuantity(),
                "reservation should release on capture (5 -> 0)");
    }

    @Test
    void duplicateWebhook_isIgnored_stockNotDecrementedTwice() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID().toString(),     // SAME event id both times
                "CAPTURE",
                "mock_ref_123",
                System.currentTimeMillis() / 1000);

        String[] sig = new String[1];
        byte[] body = signedBody(payload, sig);

        webhookService.handle(body, sig[0]);   // first delivery
        webhookService.handle(body, sig[0]);   // duplicate delivery — must be a no-op

        Product product = productRepository.findById(productId).orElseThrow();
        assertEquals(95, product.getStockQuantity(),
                "duplicate webhook must NOT decrement stock twice (still 95, not 90)");
        assertEquals(1, webhookEventRepository.count(),
                "only one webhook event row should exist");
    }

    @Test
    void tamperedSignature_isRejected_paymentUnchanged() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID().toString(),
                "payment.captured",
                "mock_ref_123",
                System.currentTimeMillis() / 1000);

        byte[] body = objectMapper.writeValueAsBytes(payload);
        String badSignature = "deadbeef";   // not a valid HMAC

        assertThrows(RuntimeException.class,
                () -> webhookService.handle(body, badSignature),
                "an invalid signature must be rejected");

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus(),
                "payment must stay PENDING when the signature is invalid");
    }

    @Test
    void staleWebhook_isRejected() throws Exception {
        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID().toString(),
                "payment.captured",
                "mock_ref_123",
                (System.currentTimeMillis() / 1000) - 3600);  // 1 hour old

        String[] sig = new String[1];
        byte[] body = signedBody(payload, sig);

        assertThrows(RuntimeException.class,
                () -> webhookService.handle(body, sig[0]),
                "a stale webhook must be rejected (replay protection)");

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
    }

    @Test
    void sameKeyTwice_createsOneOrder_reservesOnce() throws Exception {

        String sharedKey = UUID.randomUUID().toString();   // ONE key, both threads

        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go    = new CountDownLatch(1);

        // propagate the logged-in user into the worker threads
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var auth = new UsernamePasswordAuthenticationToken(
                UserDetailsImpl.build(seededUser), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        ExecutorService pool = new DelegatingSecurityContextExecutorService(
                Executors.newFixedThreadPool(threads));

        List<Throwable> errors  = Collections.synchronizedList(new ArrayList<>());
        List<Object>    results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            final int id = i;                         // add this
            pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    Object dto = this.orderService.placeOrder(sharedKey);
                    System.out.println(">>> [T" + id + "] placeOrder RETURNED (dto ok)");
                    results.add(dto);
                } catch (Throwable e) {               // Throwable, not Exception
                    e.printStackTrace();
                    errors.add(e);
                }
                return null;
            }
            );
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("threads finished in time = " + finished);   // if false, that's the story
        long orderCount = orderRepository.findAll().stream()
                .filter(o -> sharedKey.equals(o.getIdempotencyKey()))
                .count();
        Product product = productRepository.findById(productId).orElseThrow();

        // PRINT FIRST — so it shows even when an assertion fails
        System.out.println("\n===== RACE RESULT =====");
        System.out.println("successes   = " + results.size());
        System.out.println("errors      = " + errors.size());
        System.out.println("orderCount  = " + orderCount);
        System.out.println("reserved    = " + product.getReservedQuantity());
        errors.forEach(e -> System.out.println("  threw: " + e.getClass().getSimpleName() + " — " + e.getMessage()));
        System.out.println("=======================\n");

        // ASSERT SECOND
        assertEquals(1, orderCount, "same idempotency key must create exactly one order");
        assertEquals(10, product.getReservedQuantity(), "reserved should go 5 -> 10 once, not 5 -> 15");
    }

    @Test
    void testPaymentInitiation() throws Exception {

        // propagate the logged-in user into the worker threads
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var auth = new UsernamePasswordAuthenticationToken(
                UserDetailsImpl.build(seededUser), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        OrderDto orderDto = this.orderService.placeOrder(UUID.randomUUID().toString());

        PaymentDto paymentDto = this.paymentService.initiatePayment(orderDto.getOrderId(),"CASH ON DELIVERY");

        assertEquals(paymentDto.getAmount(), BigDecimal.valueOf(orderDto.getTotalAmount()), "amount should be");
        assertEquals(PaymentStatus.PENDING.toString(), paymentDto.getPaymentStaus());
        System.out.println("*********************************\n");
        System.out.println("url: " + paymentDto.getCheckoutUrl());
        System.out.println("*********************************\n");

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setId(1L);
        webhookEvent.setReceivedAt(Instant.now());
        this.webhookRepository.save(webhookEvent);

    }

    @Test
    void twoCapturesDecrementStockOnlyOnce() throws Exception {

        int threads = 2;
        CountDownLatch ready = new CountDownLatch(threads);  // both threads report "ready"
        CountDownLatch go    = new CountDownLatch(1);        // one signal fires them together
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();   // I'm ready
                go.await();          // wait for the starting gun
                try {
                    paymentService.updatePaymentStatus(paymentId, PaymentStatus.CAPTURED);
                } catch (Exception ignored) {
                    // the loser thread may throw — expected, we assert on the DB
                }
                return null;
            });
        }

        ready.await();     // wait until BOTH are ready
        go.countDown();    // release both at the same instant
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // ---- 5. proof ----
        Product product = productRepository.findById(productId).orElseThrow();

        System.out.println("\n\n========== TEST RESULT ==========");
        System.out.println("stockQuantity    = " + product.getStockQuantity()    + "  (expect 95)");
        System.out.println("reservedQuantity = " + product.getReservedQuantity() + "  (expect 0)");
        System.out.println("=================================\n\n");

        assertEquals(95, product.getStockQuantity());     // 100 - 5, ONCE
        assertEquals(0,  product.getReservedQuantity());  // 5 - 5, ONCE

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        System.out.println("paymentStatus    = " + payment.getPaymentStatus() + "  (expect CAPTURED)");
        assertEquals(PaymentStatus.CAPTURED, payment.getPaymentStatus());
    }



}
