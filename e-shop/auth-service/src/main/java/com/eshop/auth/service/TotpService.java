package com.eshop.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class TotpService {
    private static final Logger log = LoggerFactory.getLogger(TotpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TIME_STEP_SECONDS = 30;
    private static final int OTP_DIGITS = 6;
    private static final long WINDOW_STEPS = 1; // allow +- 1 step clock skew
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Value("${app.totp.dev-bypass-code:}")
    private String devBypassCode;

    public String generateSecretBase32() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpAuthUri(String issuer, String email, String base32Secret) {
        String label = issuer + ":" + email;
        return "otpauth://totp/" + urlEncode(label)
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    public boolean verifyCode(String base32Secret, String code) {
        if (code != null && code.matches("\\d{6}")
                && devBypassCode != null
                && !devBypassCode.isBlank()
                && devBypassCode.equals(code)) {
            log.warn("TOTP dev bypass code accepted. Disable app.totp.dev-bypass-code outside local testing.");
            return true;
        }
        if (base32Secret == null || base32Secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentCounter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (long i = -WINDOW_STEPS; i <= WINDOW_STEPS; i++) {
            String expected = generateCode(base32Secret, currentCounter + i);
            if (expected.equals(code)) return true;
        }
        return false;
    }

    private String generateCode(String base32Secret, long counter) {
        try {
            byte[] key = base32Decode(base32Secret);
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, OTP_DIGITS);
            return String.format("%06d", otp);
        } catch (Exception e) {
            return "";
        }
    }

    private static String urlEncode(String value) {
        return value.replace(" ", "%20").replace("@", "%40").replace(":", "%3A");
    }

    private static String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= (data[next++] & 0xff);
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = 0x1f & (buffer >> (bitsLeft - 5));
            bitsLeft -= 5;
            result.append(BASE32_ALPHABET.charAt(index));
        }
        return result.toString();
    }

    private static byte[] base32Decode(String base32) {
        String normalized = base32.replace("=", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(normalized.length() * 5 / 8 + 1);
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : normalized.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.put((byte) ((buffer >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }
        byte[] arr = new byte[out.position()];
        out.flip();
        out.get(arr);
        return arr;
    }
}
