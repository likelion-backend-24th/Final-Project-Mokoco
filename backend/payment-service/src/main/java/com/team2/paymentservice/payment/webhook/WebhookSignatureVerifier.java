package com.team2.paymentservice.payment.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class WebhookSignatureVerifier {

    private static final String SECRET_PREFIX = "whsec_";
    private static final long TOLERANCE_SECONDS = 5 * 60;

    private final byte[] secretKeyBytes;

    public WebhookSignatureVerifier(@Value("${portone.webhook-secret}") String webhookSecret) {
        String raw = webhookSecret.startsWith(SECRET_PREFIX)
                ? webhookSecret.substring(SECRET_PREFIX.length())
                : webhookSecret;
        this.secretKeyBytes = Base64.getDecoder().decode(raw);
    }

    /**
     * @param webhookId
     * @param webhookTimestamp
     * @param webhookSignature
     * @param rawBody
     */
    public boolean verify(String webhookId, String webhookTimestamp, String webhookSignature, String rawBody) {
        if (webhookId == null || webhookTimestamp == null || webhookSignature == null || rawBody == null) {
            return false;
        }

        if (!isTimestampFresh(webhookTimestamp)) {
            return false;
        }

        String signedContent = webhookId + "." + webhookTimestamp + "." + rawBody;
        String expected = sign(signedContent);

        for (String part : webhookSignature.split(" ")) {
            String[] versionAndSig = part.split(",", 2);
            if (versionAndSig.length != 2) continue;
            String candidate = versionAndSig[1];
            if (constantTimeEquals(expected, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTimestampFresh(String webhookTimestamp) {
        try {
            long ts = Long.parseLong(webhookTimestamp);
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - ts) <= TOLERANCE_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("웹훅 서명 계산에 실패했습니다.", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
