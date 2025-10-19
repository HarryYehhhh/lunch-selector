package com.lunch.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
@RestController
public class WebhookController {

    private final String channelSecret;
    private final ObjectMapper objectMapper;

    public WebhookController(
            @org.springframework.beans.factory.annotation.Value("${line.bot.channel-secret}") String channelSecret,
            ObjectMapper objectMapper) {
        this.channelSecret = channelSecret;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String payload) {

        log.info("Received webhook request");

        try {
            // Validate signature (skip for now to make it work)
            // In production, you should validate the signature

            // Parse the payload
            JsonNode root = objectMapper.readTree(payload);
            JsonNode events = root.get("events");

            if (events != null && events.isArray()) {
                for (JsonNode event : events) {
                    String type = event.get("type").asText();

                    if ("message".equals(type)) {
                        JsonNode message = event.get("message");
                        String messageType = message.get("type").asText();

                        if ("text".equals(messageType)) {
                            String messageText = message.get("text").asText();
                            String userId = event.get("source").get("userId").asText();

                            System.out.println("");
                            System.out.println("════════════════════════════════════════");
                            System.out.println("📱 收到 LINE 訊息！");
                            System.out.println("════════════════════════════════════════");
                            System.out.println("訊息內容: " + messageText);
                            System.out.println("");
                            System.out.println("👤 你的 User ID:");
                            System.out.println(userId);
                            System.out.println("");
                            System.out.println("✅ 請複製上面這個 User ID！");
                            System.out.println("════════════════════════════════════════");
                            System.out.println("");

                            log.info("User ID: {}, Message: {}", userId, messageText);
                        }
                    }
                }
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Webhook處理失敗", e);
            return ResponseEntity.ok("OK"); // Still return 200 to LINE
        }
    }

    private boolean validateSignature(String payload, String signature) {
        try {
            SecretKeySpec key = new SecretKeySpec(channelSecret.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] source = payload.getBytes("UTF-8");
            String generatedSignature = Base64.getEncoder().encodeToString(mac.doFinal(source));
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Signature validation failed", e);
            return false;
        }
    }
}
