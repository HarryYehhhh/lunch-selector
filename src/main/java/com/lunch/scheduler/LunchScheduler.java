package com.lunch.scheduler;

import com.lunch.service.LunchService;
import com.lunch.service.LineMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class LunchScheduler {

    private final LunchService lunchService;
    private final LineMessagingService lineMessagingService;

    /**
     * 每個平日 11:50 自動發送個性化午餐推薦
     * Cron 表達式: "秒 分 時 日 月 星期"
     * 0 50 11 * * 1-5 = 週一到週五的 11:50:00
     */
    @Scheduled(cron = "0 50 11 * * *", zone = "Asia/Taipei")
    public void sendDailyLunchNotification() {
        String currentTime = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("⏰ 定時任務觸發: {}", currentTime);

        try {
            // 發送個性化推薦給每個啟用通知的用戶
            lineMessagingService.sendPersonalizedLunchNotifications();

            log.info("✅ 定時個性化午餐通知發送成功");

        } catch (Exception e) {
            log.error("❌ 定時午餐通知發送失敗", e);
        }
    }
}
