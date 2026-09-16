package com.ecomm.sb_ecomm.payment.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class SignatureUtil {

    private SignatureUtil() {}

    public static String hmacSha256(byte[] body , String secret){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body);
            return HexFormat.of().formatHex(raw);

        }catch (Exception e){
            throw new IllegalStateException("Failed to compute HMAC" , e);
        }
    }

    public static boolean constantTimeEquals(String a , String b){
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
