package com.lunch.service;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineMessagingService {
    
    private final LineMessagingClient lineMessagingClient;
    
    @Value("${lunch.user-ids}")
    private List<String> userIds;
    
    public void sendLunchNotification(String message) {
        if (userIds == null || userIds.isEmpty()) {
            log.error("❌ 錯誤：沒有設定 User ID！請先發送訊息給 Bot 以取得你的 User ID");
            System.out.println("");
            System.out.println("════════════════════════════════════════");
            System.out.println("❌ 錯誤：沒有設定 LINE User ID");
            System.out.println("════════════════════════════════════════");
            System.out.println("請按照以下步驟取得 User ID：");
            System.out.println("1. 加入你的 LINE Bot 好友");
            System.out.println("2. 傳送任何訊息給 Bot");
            System.out.println("3. 查看終端機輸出的 User ID");
            System.out.println("4. 將 User ID 加入啟動參數：");
            System.out.println("   --lunch.user-ids=你的UserID");
            System.out.println("════════════════════════════════════════");
            System.out.println("");
            return;
        }

        TextMessage textMessage = new TextMessage(message);

        for (String userId : userIds) {
            try {
                log.info("正在發送訊息給使用者: {}", userId);
                CompletableFuture<BotApiResponse> future = lineMessagingClient.pushMessage(
                    new PushMessage(userId, textMessage)
                );
                BotApiResponse response = future.get();
                log.info("✅ 已成功發送訊息給使用者: {}", userId);
            } catch (Exception e) {
                log.error("❌ 發送訊息失敗 (userId: {}): {}", userId, e.getMessage(), e);
            }
        }
    }
}
