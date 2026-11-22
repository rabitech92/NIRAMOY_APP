package com.niramoy.pharmacy.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** মোবাইল অ্যাপের জন্য HS256 JWT (বাইরের লাইব্রেরি ছাড়া) */
@Component
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long ttlSeconds;
    private final ObjectMapper mapper;

    public JwtService(@Value("${niramoy.api.secret:}") String configured,
                      @Value("${niramoy.api.token-hours:336}") long hours, ObjectMapper mapper) {
        this.mapper = mapper;
        this.ttlSeconds = hours * 3600;
        if (configured == null || configured.length() < 32) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.secret = random;
            log.warn("niramoy.api.secret সেট করা নেই (বা ৩২ অক্ষরের কম) — অস্থায়ী কী ব্যবহার হচ্ছে; "
                    + "সার্ভার রিস্টার্ট করলে সব মোবাইল ব্যবহারকারীকে আবার লগইন করতে হবে।");
        } else {
            this.secret = configured.getBytes(StandardCharsets.UTF_8);
        }
    }

    public long getTtlSeconds() { return ttlSeconds; }

    public String issue(UserPrincipal user) {
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(user.getId()));
            payload.put("role", user.getRole().name());
            payload.put("iat", now);
            payload.put("exp", now + ttlSeconds);
            String head = B64.encodeToString(mapper.writeValueAsBytes(header));
            String body = B64.encodeToString(mapper.writeValueAsBytes(payload));
            return head + "." + body + "." + sign(head + "." + body);
        } catch (Exception e) {
            throw new IllegalStateException("টোকেন তৈরি করা যায়নি", e);
        }
    }

    /** বৈধ হলে ব্যবহারকারীর id, নইলে null */
    public Long verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            byte[] expected = sign(parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, parts[2].getBytes(StandardCharsets.UTF_8))) return null;
            Map<String, Object> payload = mapper.readValue(B64D.decode(parts[1]), new TypeReference<>() { });
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) return null;
            return Long.parseLong(String.valueOf(payload.get("sub")));
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return B64.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
