package com.ecomm.sb_ecomm.security;

import com.ecomm.sb_ecomm.auth.jwt.JwtUtils;
import com.ecomm.sb_ecomm.auth.services.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private static final String COOKIE_NAME = "springE-ComApp";

    private JwtUtils jwtUtils;
    private String secret;

    @BeforeEach
    void setUp() {
        secret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationDate", 3_000_000);
        ReflectionTestUtils.setField(jwtUtils, "jwtCookie", COOKIE_NAME);
    }

    private UserDetailsImpl principal() {
        return new UserDetailsImpl(1L, "user1", "user1@example.com", "x",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void validToken_isAccepted() {
        String token = jwtUtils.generateTokenFromUsername("user1");

        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getUsernameFromJwtToken(token)).isEqualTo("user1");
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected_withoutThrowing() {
        String forged = Jwts.builder()
                .subject("admin")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                        Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded()))))
                .compact();

        assertThat(jwtUtils.validateToken(forged)).isFalse();
    }

    @Test
    void expiredToken_isRejected() {
        String expired = Jwts.builder()
                .subject("user1")
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();

        assertThat(jwtUtils.validateToken(expired)).isFalse();
    }

    @Test
    void readingTheCookie_doesNotWriteTheTokenToStdout() {
        String token = jwtUtils.generateTokenFromUsername("user1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, token));

        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            assertThat(jwtUtils.getJwtFromCookie(request)).isEqualTo(token);
            jwtUtils.validateToken(token);
        } finally {
            System.setOut(original);
        }

        assertThat(captured.toString()).doesNotContain(token);
    }

    @Test
    void authCookie_isHttpOnlySecureSameSite_andLivesAsLongAsTheToken() {
        ReflectionTestUtils.setField(jwtUtils, "cookieSecure", true);

        ResponseCookie cookie = jwtUtils.generateJwtCookie(principal());

        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3_000L); // 3_000_000 ms
    }

    @Test
    void cleanCookie_expiresImmediately() {
        ReflectionTestUtils.setField(jwtUtils, "cookieSecure", true);

        ResponseCookie cookie = jwtUtils.getCleanCookie();

        assertThat(cookie.getMaxAge().getSeconds()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getValue()).isEmpty();
    }
}
