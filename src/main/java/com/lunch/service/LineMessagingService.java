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
        TextMessage textMessage = new TextMessage(message);
        
        for (String userId : userIds) {
            try {
                CompletableFuture<BotApiResponse> future = lineMessagingClient.pushMessage(
                    new PushMessage(userId, textMessage)
                );
                future.get();
                log.info("已發送訊息給使用者: {}", userId);
            } catch (Exception e) {
                log.error("發送訊息失敗 (userId: {})", userId, e);
            }
        }
    }
}
