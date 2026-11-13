package com.ecomm.sb_ecomm.auth.jwt;

import com.ecomm.sb_ecomm.auth.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtils {

    public static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationDate;

    @Value("${spring.app.ecom.jwtCookeName}")
    private String jwtCookie;

    // Secure by default; only the dev profile turns it off (plain-HTTP localhost).
    @Value("${spring.app.ecom.cookieSecure:true}")
    private boolean cookieSecure;


    private Key key(){return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));}

    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
    }

    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + this.jwtExpirationDate))
                .signWith(key())
                .compact();
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = this.generateTokenFromUsername(userPrincipal.getUsername());

        // The cookie lives exactly as long as the token inside it.
        return baseCookie(jwt)
                .maxAge(Duration.ofMillis(this.jwtExpirationDate))
                .build();
    }

    public ResponseCookie getCleanCookie(){
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    // HttpOnly: JavaScript (and therefore XSS) cannot read the token.
    // SameSite=Lax: the cookie is not sent on cross-site POSTs (CSRF is disabled for this stateless API).
    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie
                .from(jwtCookie, value)
                .path("/api")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax");
    }

    public String getUsernameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) this.key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Boolean validateToken(String token){
        try{
            Jwts.parser()
                    .verifyWith((SecretKey) this.key())
                    .build()
                    .parseSignedClaims(token);
            return true;
        }catch (ExpiredJwtException e)     { logger.error("Expired JWT token : {}" , e.getMessage());}
        catch (MalformedJwtException e)    { logger.error("Invalid JWT token : {}" , e.getMessage());}
        catch (UnsupportedJwtException e)  { logger.error("Unsupported JWT token : {}" , e.getMessage());}
        // Anything else the parser rejects, notably a bad signature (SignatureException).
        catch (JwtException e)             { logger.error("JWT validation failed : {}" , e.getMessage());}
        catch (IllegalArgumentException e) { logger.error("JWT claims string is empty: {}" , e.getMessage());}

        return false;
    }

}
