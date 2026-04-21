package com.ecomm.sb_ecomm.security.jwt;

import com.ecomm.sb_ecomm.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
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


    private Key key(){return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));}

    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if (cookie != null) {
            System.out.println(cookie.getName() + " : " + cookie.getValue());
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

        return ResponseCookie
                .from(jwtCookie,jwt)
                .path("/api")
                .maxAge(24 * 60 * 60)
                .httpOnly(false)
                .build();
    }

    public ResponseCookie getCleanCookie(){
        return ResponseCookie
                .from(jwtCookie,null)
                .path("/api")
                .build();
    }

    public String getUsernameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) this.key())
                .build()
                .parseSignedClaims(token)
                .getBody().getSubject();
    }

    public Boolean validateToken(String token){
        try{
            System.out.println("Validating Token");
            Jwts.parser()
                    .verifyWith((SecretKey) this.key())
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (MalformedJwtException e)   { logger.error("Invalid JWT token : {}" , e.getMessage());}
        catch (ExpiredJwtException e)      { logger.error("Expired JWT token : {}" , e.getMessage());}
        catch (UnsupportedJwtException e)  { logger.error("Unsupported JWT token : {}" , e.getMessage());}
        catch (IllegalArgumentException e) { logger.error("JWT claims string is empty: {}" , e.getMessage());}

        return false;
    }

}
