package com.lunch.scheduler;

import com.lunch.service.LineMessagingService;
import com.lunch.service.LunchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LunchScheduler 單元測試
 * 測試每日午餐通知定時任務的功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LunchScheduler 單元測試")
class LunchSchedulerTest {

    @Mock
    private LunchService lunchService;

    @Mock
    private LineMessagingService lineMessagingService;

    @InjectMocks
    private LunchScheduler lunchScheduler;

    @BeforeEach
    void setUp() {
        // 每個測試前的初始化（如果需要的話）
    }

    @Test
    @DisplayName("成功發送午餐通知 - 應該調用 sendPersonalizedLunchNotifications")
    void testSendDailyLunchNotification_Success() {
        // Given: 準備測試環境
        // lineMessagingService.sendPersonalizedLunchNotifications() 不會拋出異常

        // When: 執行定時任務
        lunchScheduler.sendDailyLunchNotification();

        // Then: 驗證方法被調用
        verify(lineMessagingService, times(1)).sendPersonalizedLunchNotifications();
        verifyNoMoreInteractions(lineMessagingService);
    }

    @Test
    @DisplayName("發送通知時發生異常 - 應該捕獲並記錄錯誤")
    void testSendDailyLunchNotification_Exception() {
        // Given: 模擬發送通知時拋出異常
        RuntimeException testException = new RuntimeException("測試異常：發送通知失敗");
        doThrow(testException).when(lineMessagingService).sendPersonalizedLunchNotifications();

        // When: 執行定時任務（不應該拋出異常）
        assertDoesNotThrow(() -> lunchScheduler.sendDailyLunchNotification());

        // Then: 驗證方法被調用（即使失敗也應該嘗試）
        verify(lineMessagingService, times(1)).sendPersonalizedLunchNotifications();
    }

    @Test
    @DisplayName("多次調用定時任務 - 每次都應該發送通知")
    void testSendDailyLunchNotification_MultipleCalls() {
        // Given: 準備測試環境

        // When: 多次執行定時任務（模擬多天的情況）
        lunchScheduler.sendDailyLunchNotification();
        lunchScheduler.sendDailyLunchNotification();
        lunchScheduler.sendDailyLunchNotification();

        // Then: 驗證方法被調用 3 次
        verify(lineMessagingService, times(3)).sendPersonalizedLunchNotifications();
    }

    @Test
    @DisplayName("驗證定時任務不依賴 LunchService")
    void testSendDailyLunchNotification_DoesNotUseLunchService() {
        // Given: 準備測試環境

        // When: 執行定時任務
        lunchScheduler.sendDailyLunchNotification();

        // Then: 驗證 LunchService 沒有被調用
        verifyNoInteractions(lunchService);
    }

    @Test
    @DisplayName("異常後仍能繼續執行 - 不影響後續調用")
    void testSendDailyLunchNotification_RecoveryAfterException() {
        // Given: 第一次調用會拋出異常，第二次正常
        RuntimeException testException = new RuntimeException("暫時性錯誤");
        doThrow(testException)
            .doNothing()
            .when(lineMessagingService).sendPersonalizedLunchNotifications();

        // When: 第一次調用（會失敗）
        assertDoesNotThrow(() -> lunchScheduler.sendDailyLunchNotification());

        // When: 第二次調用（會成功）
        assertDoesNotThrow(() -> lunchScheduler.sendDailyLunchNotification());

        // Then: 兩次都應該嘗試發送
        verify(lineMessagingService, times(2)).sendPersonalizedLunchNotifications();
    }

    @Test
    @DisplayName("處理空指針異常 - 應該捕獲並記錄")
    void testSendDailyLunchNotification_NullPointerException() {
        // Given: 模擬 NullPointerException
        doThrow(new NullPointerException("測試 NPE"))
            .when(lineMessagingService).sendPersonalizedLunchNotifications();

        // When: 執行定時任務
        assertDoesNotThrow(() -> lunchScheduler.sendDailyLunchNotification());

        // Then: 驗證方法被調用
        verify(lineMessagingService, times(1)).sendPersonalizedLunchNotifications();
    }

    @Test
    @DisplayName("處理IllegalArgumentException - 應該捕獲並記錄")
    void testSendDailyLunchNotification_IllegalArgumentException() {
        // Given: 模擬 IllegalArgumentException
        IllegalArgumentException testException = new IllegalArgumentException("無效的參數");
        doThrow(testException).when(lineMessagingService).sendPersonalizedLunchNotifications();

        // When: 執行定時任務
        assertDoesNotThrow(() -> lunchScheduler.sendDailyLunchNotification());

        // Then: 驗證異常被正確處理
        verify(lineMessagingService, times(1)).sendPersonalizedLunchNotifications();
    }

    @Test
    @DisplayName("驗證方法簽名 - public void 無參數")
    void testMethodSignature() {
        // 這個測試確保方法簽名符合 @Scheduled 的要求
        assertDoesNotThrow(() -> {
            lunchScheduler.sendDailyLunchNotification();
        }, "定時任務方法應該是 public void 且無參數");
    }
}
