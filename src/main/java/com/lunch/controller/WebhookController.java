package com.lunch.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunch.model.User;
import com.lunch.service.LunchService;
import com.lunch.service.LineMessagingService;
import com.lunch.service.UserStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class WebhookController {

    private final String channelSecret;
    private final ObjectMapper objectMapper;
    private final UserStorageService userStorageService;
    private final LineMessagingService lineMessagingService;
    private final LunchService lunchService;

    public WebhookController(
            @org.springframework.beans.factory.annotation.Value("${line.bot.channel-secret}") String channelSecret,
            ObjectMapper objectMapper,
            UserStorageService userStorageService,
            LineMessagingService lineMessagingService,
            LunchService lunchService) {
        this.channelSecret = channelSecret;
        this.objectMapper = objectMapper;
        this.userStorageService = userStorageService;
        this.lineMessagingService = lineMessagingService;
        this.lunchService = lunchService;
    }

    @PostMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String payload) {

        log.info("Received webhook request");

        try {
            // Validate signature to ensure request is from LINE
            if (signature != null && !validateSignature(payload, signature)) {
                log.warn("⚠️ Invalid signature - possible unauthorized request");
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // Parse the payload
            JsonNode root = objectMapper.readTree(payload);
            JsonNode events = root.get("events");

            if (events != null && events.isArray()) {
                for (JsonNode event : events) {
                    String type = event.get("type").asText();

                    // 處理加好友事件
                    if ("follow".equals(type)) {
                        String userId = event.get("source").get("userId").asText();
                        boolean isNewUser = userStorageService.addUser(userId);

                        if (isNewUser) {
                            System.out.println("");
                            System.out.println("════════════════════════════════════════");
                            System.out.println("🎉 新用戶加入！");
                            System.out.println("════════════════════════════════════════");
                            System.out.println("👤 User ID: " + userId);
                            System.out.println("📊 總用戶數: " + userStorageService.getUserCount());
                            System.out.println("════════════════════════════════════════");
                            System.out.println("");
                        }

                        log.info("Follow event - User ID: {}, New user: {}", userId, isNewUser);
                    }

                    // 處理訊息事件
                    else if ("message".equals(type)) {
                        JsonNode message = event.get("message");
                        String messageType = message.get("type").asText();

                        if ("text".equals(messageType)) {
                            String messageText = message.get("text").asText();
                            String userId = event.get("source").get("userId").asText();

                            // 自動註冊用戶
                            boolean isNewUser = userStorageService.addUser(userId);

                            System.out.println("");
                            System.out.println("════════════════════════════════════════");
                            System.out.println("📱 收到 LINE 訊息！");
                            System.out.println("════════════════════════════════════════");
                            System.out.println("訊息內容: " + messageText);
                            System.out.println("");
                            System.out.println("👤 User ID:");
                            System.out.println(userId);
                            System.out.println("");
                            if (isNewUser) {
                                System.out.println("🆕 新用戶已自動註冊！");
                            } else {
                                System.out.println("✅ 用戶已存在");
                            }
                            System.out.println("📊 總用戶數: " + userStorageService.getUserCount());
                            System.out.println("════════════════════════════════════════");
                            System.out.println("");

                            log.info("User ID: {}, Message: {}, New user: {}", userId, messageText, isNewUser);

                            // 處理用戶指令
                            handleUserCommand(userId, messageText);
                        }
                    }

                    // 處理取消好友事件
                    else if ("unfollow".equals(type)) {
                        String userId = event.get("source").get("userId").asText();
                        boolean removed = userStorageService.removeUser(userId);

                        if (removed) {
                            log.info("Unfollow event - User ID: {} removed", userId);
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

    /**
     * 處理用戶指令
     */
    private void handleUserCommand(String userId, String message) {
        String lowerMessage = message.toLowerCase().trim();

        try {
            // 幫助指令
            if (lowerMessage.equals("幫助") || lowerMessage.equals("help") || lowerMessage.equals("指令")) {
                sendHelpMessage(userId);
            }
            // 查看所有餐廳
            else if (lowerMessage.equals("查看餐廳") || lowerMessage.equals("餐廳列表")) {
                sendRestaurantList(userId);
            }
            // 查看個人偏好
            else if (lowerMessage.equals("查看偏好") || lowerMessage.equals("我的偏好")) {
                sendUserPreferences(userId);
            }
            // 設定偏好餐廳：設定 麥當勞,便當店,麵店
            else if (message.startsWith("設定 ") || message.startsWith("设定 ")) {
                String restaurantsStr = message.substring(3).trim();
                setUserRestaurants(userId, restaurantsStr);
            }
            // 排除餐廳：排除 壽司,火鍋
            else if (message.startsWith("排除 ")) {
                String excludeStr = message.substring(3).trim();
                excludeRestaurants(userId, excludeStr);
            }
            // 清除偏好
            else if (lowerMessage.equals("清除偏好") || lowerMessage.equals("重置")) {
                clearUserPreferences(userId);
            }
            // 開啟通知
            else if (lowerMessage.equals("開啟通知") || lowerMessage.equals("启用通知") || lowerMessage.equals("打開通知")) {
                enableNotification(userId);
            }
            // 關閉通知
            else if (lowerMessage.equals("關閉通知") || lowerMessage.equals("关闭通知")) {
                disableNotification(userId);
            }
            // 立即推薦
            else if (lowerMessage.equals("推薦") || lowerMessage.equals("午餐") || lowerMessage.equals("吃什麼")) {
                sendImmediateRecommendation(userId);
            }
            else {
                // 不是指令，忽略或發送預設回覆
                log.debug("收到非指令訊息: {}", message);
            }
        } catch (Exception e) {
            log.error("處理用戶指令時發生錯誤", e);
            lineMessagingService.sendMessageToUser(userId, "❌ 處理指令時發生錯誤，請稍後再試");
        }
    }

    private void sendHelpMessage(String userId) {
        String helpText = """
                📖 午餐選擇器使用說明

                🍽️ 基本指令：
                • 推薦 / 午餐 / 吃什麼 - 立即獲得推薦
                • 查看餐廳 - 查看所有可用餐廳
                • 查看偏好 - 查看你的偏好設定

                ⚙️ 設定指令：
                • 設定 餐廳1,餐廳2,餐廳3
                  (只從這些餐廳中推薦)
                • 排除 餐廳1,餐廳2
                  (不推薦這些餐廳)
                • 清除偏好 - 清除所有設定

                🔔 通知控制：
                • 開啟通知 - 啟用定時通知
                • 關閉通知 - 停止定時通知

                💡 範例：
                設定 麥當勞,肯德基,便當店
                排除 壽司,火鍋
                """;

        lineMessagingService.sendMessageToUser(userId, helpText);
        log.info("已發送幫助訊息給用戶: {}", userId);
    }

    private void sendRestaurantList(String userId) {
        List<String> restaurants = lunchService.getAllRestaurants();

        String message = "🍽️ 所有可用餐廳：\n\n" +
                String.join("\n", restaurants) +
                "\n\n共 " + restaurants.size() + " 家餐廳";

        lineMessagingService.sendMessageToUser(userId, message);
        log.info("已發送餐廳列表給用戶: {}", userId);
    }

    private void sendUserPreferences(String userId) {
        Optional<User> userOpt = userStorageService.getUser(userId);

        if (userOpt.isEmpty()) {
            lineMessagingService.sendMessageToUser(userId, "❌ 找不到你的資料");
            return;
        }

        User user = userOpt.get();
        StringBuilder message = new StringBuilder("⚙️ 你的偏好設定：\n\n");

        // 通知狀態
        boolean notificationEnabled = user.getPreferences().getNotificationEnabled() != null ?
                user.getPreferences().getNotificationEnabled() : true;
        message.append("🔔 通知狀態：").append(notificationEnabled ? "已開啟" : "已關閉").append("\n\n");

        // 偏好餐廳
        List<String> restaurants = user.getPreferences().getRestaurants();
        if (restaurants != null && !restaurants.isEmpty()) {
            message.append("✅ 偏好餐廳：\n");
            restaurants.forEach(r -> message.append("  • ").append(r).append("\n"));
        } else {
            message.append("✅ 偏好餐廳：全部\n");
        }

        message.append("\n");

        // 排除餐廳
        List<String> excludeRestaurants = user.getPreferences().getExcludeRestaurants();
        if (excludeRestaurants != null && !excludeRestaurants.isEmpty()) {
            message.append("❌ 排除餐廳：\n");
            excludeRestaurants.forEach(r -> message.append("  • ").append(r).append("\n"));
        } else {
            message.append("❌ 排除餐廳：無\n");
        }

        lineMessagingService.sendMessageToUser(userId, message.toString());
        log.info("已發送偏好設定給用戶: {}", userId);
    }

    private void setUserRestaurants(String userId, String restaurantsStr) {
        // 解析餐廳列表
        List<String> restaurants = Arrays.stream(restaurantsStr.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (restaurants.isEmpty()) {
            lineMessagingService.sendMessageToUser(userId, "❌ 請提供至少一家餐廳\n\n範例：設定 麥當勞,便當店,麵店");
            return;
        }

        boolean success = userStorageService.setUserRestaurants(userId, restaurants);

        if (success) {
            String message = "✅ 已設定偏好餐廳：\n\n" +
                    String.join("\n", restaurants) +
                    "\n\n今後只會從這些餐廳中推薦";
            lineMessagingService.sendMessageToUser(userId, message);
            log.info("用戶 {} 已設定偏好餐廳: {}", userId, restaurants);
        } else {
            lineMessagingService.sendMessageToUser(userId, "❌ 設定失敗，請稍後再試");
        }
    }

    private void excludeRestaurants(String userId, String excludeStr) {
        // 解析排除列表
        List<String> excludeRestaurants = Arrays.stream(excludeStr.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (excludeRestaurants.isEmpty()) {
            lineMessagingService.sendMessageToUser(userId, "❌ 請提供至少一家要排除的餐廳\n\n範例：排除 壽司,火鍋");
            return;
        }

        Optional<User> userOpt = userStorageService.getUser(userId);
        if (userOpt.isEmpty()) {
            lineMessagingService.sendMessageToUser(userId, "❌ 找不到你的資料");
            return;
        }

        User user = userOpt.get();
        user.getPreferences().setExcludeRestaurants(excludeRestaurants);

        boolean success = userStorageService.updateUserPreferences(userId, user.getPreferences());

        if (success) {
            String message = "✅ 已設定排除餐廳：\n\n" +
                    String.join("\n", excludeRestaurants) +
                    "\n\n這些餐廳不會被推薦";
            lineMessagingService.sendMessageToUser(userId, message);
            log.info("用戶 {} 已設定排除餐廳: {}", userId, excludeRestaurants);
        } else {
            lineMessagingService.sendMessageToUser(userId, "❌ 設定失敗，請稍後再試");
        }
    }

    private void clearUserPreferences(String userId) {
        Optional<User> userOpt = userStorageService.getUser(userId);
        if (userOpt.isEmpty()) {
            lineMessagingService.sendMessageToUser(userId, "❌ 找不到你的資料");
            return;
        }

        User user = userOpt.get();
        user.getPreferences().setRestaurants(Arrays.asList());
        user.getPreferences().setExcludeRestaurants(Arrays.asList());

        boolean success = userStorageService.updateUserPreferences(userId, user.getPreferences());

        if (success) {
            lineMessagingService.sendMessageToUser(userId, "✅ 已清除所有偏好設定\n\n將從所有餐廳中推薦");
            log.info("用戶 {} 已清除偏好設定", userId);
        } else {
            lineMessagingService.sendMessageToUser(userId, "❌ 清除失敗，請稍後再試");
        }
    }

    private void enableNotification(String userId) {
        boolean success = userStorageService.enableNotification(userId);

        if (success) {
            lineMessagingService.sendMessageToUser(userId, "🔔 已開啟通知\n\n每個工作日 11:50 會收到午餐推薦");
            log.info("用戶 {} 已開啟通知", userId);
        } else {
            lineMessagingService.sendMessageToUser(userId, "❌ 設定失敗，請稍後再試");
        }
    }

    private void disableNotification(String userId) {
        boolean success = userStorageService.disableNotification(userId);

        if (success) {
            lineMessagingService.sendMessageToUser(userId, "🔕 已關閉通知\n\n將不再收到定時推薦");
            log.info("用戶 {} 已關閉通知", userId);
        } else {
            lineMessagingService.sendMessageToUser(userId, "❌ 設定失敗，請稍後再試");
        }
    }

    private void sendImmediateRecommendation(String userId) {
        Optional<User> userOpt = userStorageService.getUser(userId);

        if (userOpt.isEmpty()) {
            lineMessagingService.sendMessageToUser(userId, "❌ 找不到你的資料");
            return;
        }

        User user = userOpt.get();
        String restaurant = lunchService.selectRestaurantForUser(user);
        String message = lunchService.formatLunchMessageForUser(restaurant, user);

        lineMessagingService.sendMessageToUser(userId, message);
        log.info("已發送即時推薦給用戶 {}: {}", userId, restaurant);
    }
}
