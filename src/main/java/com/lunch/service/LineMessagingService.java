package com.lunch.service;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import com.lunch.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineMessagingService {

    private final LineMessagingClient lineMessagingClient;
    private final UserStorageService userStorageService;
    private final LunchService lunchService;
    
    /**
     * 發送午餐通知（個性化推薦）
     * 根據每個用戶的偏好設定發送不同的推薦
     */
    public void sendPersonalizedLunchNotifications() {
        // 獲取所有啟用通知的用戶
        List<User> users = userStorageService.getNotificationEnabledUsers();

        if (users == null || users.isEmpty()) {
            log.warn("⚠️ 沒有啟用通知的用戶！");
            System.out.println("");
            System.out.println("════════════════════════════════════════");
            System.out.println("⚠️ 目前沒有啟用通知的用戶");
            System.out.println("════════════════════════════════════════");
            System.out.println("請按照以下步驟註冊用戶：");
            System.out.println("1. 加入你的 LINE Bot 好友");
            System.out.println("2. 傳送任何訊息給 Bot");
            System.out.println("3. 系統會自動註冊你的 User ID");
            System.out.println("");
            System.out.println("💡 用戶資料存儲在 Firestore 中");
            System.out.println("════════════════════════════════════════");
            System.out.println("");
            return;
        }

        log.info("準備發送個性化通知給 {} 位用戶", users.size());

        int successCount = 0;
        int failCount = 0;

        for (User user : users) {
            try {
                // 根據用戶偏好選擇餐廳
                String restaurant = lunchService.selectRestaurantForUser(user);

                // 生成個性化訊息
                String message = lunchService.formatLunchMessageForUser(restaurant, user);

                // 發送訊息
                TextMessage textMessage = new TextMessage(message);
                CompletableFuture<BotApiResponse> future = lineMessagingClient.pushMessage(
                    new PushMessage(user.getUserId(), textMessage)
                );
                future.get();

                log.info("✅ 已發送個性化推薦給使用者: {} (餐廳: {})", user.getUserId(), restaurant);
                successCount++;
            } catch (Exception e) {
                log.error("❌ 發送訊息失敗 (userId: {}): {}", user.getUserId(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("📊 通知發送完成 - 成功: {}, 失敗: {}, 總計: {}",
            successCount, failCount, users.size());
    }

    /**
     * 發送統一訊息給所有用戶（舊版 API，保留向後兼容）
     *
     * @param message 訊息內容
     */
    @Deprecated
    public void sendLunchNotification(String message) {
        List<User> users = userStorageService.getAllActiveUsers();

        if (users == null || users.isEmpty()) {
            log.warn("⚠️ 沒有註冊的用戶！");
            return;
        }

        log.info("準備發送統一訊息給 {} 位用戶", users.size());
        TextMessage textMessage = new TextMessage(message);

        int successCount = 0;
        int failCount = 0;

        for (User user : users) {
            try {
                CompletableFuture<BotApiResponse> future = lineMessagingClient.pushMessage(
                    new PushMessage(user.getUserId(), textMessage)
                );
                future.get();
                log.info("✅ 已成功發送訊息給使用者: {}", user.getUserId());
                successCount++;
            } catch (Exception e) {
                log.error("❌ 發送訊息失敗 (userId: {}): {}", user.getUserId(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("📊 通知發送完成 - 成功: {}, 失敗: {}, 總計: {}",
            successCount, failCount, users.size());
    }

    /**
     * 發送訊息給單一用戶
     *
     * @param userId  用戶 ID
     * @param message 訊息內容
     * @return true 如果成功，false 如果失敗
     */
    public boolean sendMessageToUser(String userId, String message) {
        try {
            TextMessage textMessage = new TextMessage(message);
            CompletableFuture<BotApiResponse> future = lineMessagingClient.pushMessage(
                new PushMessage(userId, textMessage)
            );
            future.get();
            log.info("✅ 已發送訊息給使用者: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("❌ 發送訊息失敗 (userId: {}): {}", userId, e.getMessage(), e);
            return false;
        }
    }
}
