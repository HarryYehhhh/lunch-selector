package com.lunch.scheduler;

import com.lunch.service.LunchService;
import com.lunch.service.LineMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 測試用 Scheduler - 每分鐘執行一次
 * 啟動時加上 --spring.profiles.active=test 來啟用
 * 測試完成後請刪除此檔案或移除 @Component 註解
 */
@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class TestScheduler {

    private final LunchService lunchService;
    private final LineMessagingService lineMessagingService;

    /**
     * 測試用：每分鐘執行一次
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Taipei")
    public void testScheduledTask() {
        String currentTime = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("🧪 測試定時任務觸發: {}", currentTime);

        try {
            String restaurant = lunchService.selectRandomRestaurant();
            String message = "【測試通知】\n" + lunchService.formatLunchMessage(restaurant);

            log.info("📋 測試午餐選擇: {}", restaurant);

            lineMessagingService.sendLunchNotification(message);

            log.info("✅ 測試通知發送成功");

        } catch (Exception e) {
            log.error("❌ 測試通知發送失敗", e);
        }
    }
}
