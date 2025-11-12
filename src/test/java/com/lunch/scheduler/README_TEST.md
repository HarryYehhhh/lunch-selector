# LunchScheduler 單元測試說明

## 📋 測試概覽

`LunchSchedulerTest.java` 為 `LunchScheduler` 類提供完整的單元測試覆蓋。

## ✅ 測試結果

```
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0
```

**測試覆蓋率**: 100%

---

## 🧪 測試案例說明

### 1. `testSendDailyLunchNotification_Success`
**測試目的**: 驗證正常情況下發送通知功能

**測試內容**:
- 調用 `sendDailyLunchNotification()`
- 驗證 `lineMessagingService.sendPersonalizedLunchNotifications()` 被調用一次
- 確保沒有其他不必要的方法調用

**預期結果**: ✅ 通知成功發送

---

### 2. `testSendDailyLunchNotification_Exception`
**測試目的**: 驗證異常處理機制

**測試內容**:
- 模擬 `lineMessagingService.sendPersonalizedLunchNotifications()` 拋出 RuntimeException
- 執行定時任務
- 驗證異常被捕獲，不會向上拋出

**預期結果**: ✅ 異常被正確捕獲和記錄

---

### 3. `testSendDailyLunchNotification_MultipleCalls`
**測試目的**: 驗證多次調用的穩定性

**測試內容**:
- 連續調用定時任務 3 次
- 驗證每次都正確執行

**預期結果**: ✅ 每次調用都獨立執行

---

### 4. `testSendDailyLunchNotification_DoesNotUseLunchService`
**測試目的**: 驗證依賴隔離

**測試內容**:
- 執行定時任務
- 確認 `lunchService` 沒有被使用

**預期結果**: ✅ 確認不依賴 LunchService

---

### 5. `testSendDailyLunchNotification_RecoveryAfterException`
**測試目的**: 驗證錯誤恢復能力

**測試內容**:
- 第一次調用模擬失敗
- 第二次調用正常執行
- 驗證兩次都嘗試發送

**預期結果**: ✅ 失敗後可以恢復

---

### 6. `testSendDailyLunchNotification_NullPointerException`
**測試目的**: 驗證空指針異常處理

**測試內容**:
- 模擬 NullPointerException
- 驗證異常被正確處理

**預期結果**: ✅ NPE 被捕獲並記錄

---

### 7. `testSendDailyLunchNotification_IllegalArgumentException`
**測試目的**: 驗證參數異常處理

**測試內容**:
- 模擬 IllegalArgumentException
- 驗證異常被正確處理

**預期結果**: ✅ 參數異常被捕獲並記錄

---

### 8. `testMethodSignature`
**測試目的**: 驗證方法簽名符合 @Scheduled 要求

**測試內容**:
- 確認方法是 public
- 確認方法返回 void
- 確認方法無參數

**預期結果**: ✅ 方法簽名正確

---

## 🎯 測試策略

### 使用的測試框架和工具

1. **JUnit 5** - 測試框架
   - `@Test` - 標記測試方法
   - `@DisplayName` - 提供可讀的測試名稱
   - `@BeforeEach` - 測試前準備

2. **Mockito** - Mock 框架
   - `@Mock` - 創建 mock 物件
   - `@InjectMocks` - 自動注入 mock
   - `@ExtendWith(MockitoExtension.class)` - 啟用 Mockito

3. **斷言方法**
   - `verify()` - 驗證方法調用
   - `assertDoesNotThrow()` - 驗證不拋出異常
   - `times()` - 驗證調用次數

---

## 📊 測試覆蓋的場景

| 場景 | 測試案例 | 狀態 |
|------|---------|------|
| 正常發送 | testSendDailyLunchNotification_Success | ✅ |
| 運行時異常 | testSendDailyLunchNotification_Exception | ✅ |
| 空指針異常 | testSendDailyLunchNotification_NullPointerException | ✅ |
| 參數異常 | testSendDailyLunchNotification_IllegalArgumentException | ✅ |
| 多次調用 | testSendDailyLunchNotification_MultipleCalls | ✅ |
| 錯誤恢復 | testSendDailyLunchNotification_RecoveryAfterException | ✅ |
| 依賴隔離 | testSendDailyLunchNotification_DoesNotUseLunchService | ✅ |
| 方法簽名 | testMethodSignature | ✅ |

---

## 🔍 代碼覆蓋率分析

### LunchScheduler.java 覆蓋情況

```java
@Scheduled(cron = "0 50 11 * * 1-5", zone = "Asia/Taipei")
public void sendDailyLunchNotification() {
    String currentTime = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    log.info("⏰ 定時任務觸發: {}", currentTime);  // ✅ 已測試

    try {
        lineMessagingService.sendPersonalizedLunchNotifications();  // ✅ 已測試
        log.info("✅ 定時個性化午餐通知發送成功");  // ✅ 已測試
    } catch (Exception e) {
        log.error("❌ 定時午餐通知發送失敗", e);  // ✅ 已測試
    }
}
```

**覆蓋率**: 100% ✅

---

## 🚀 運行測試

### 運行所有測試
```bash
mvn test
```

### 運行特定測試類
```bash
mvn test -Dtest=LunchSchedulerTest
```

### 運行特定測試方法
```bash
mvn test -Dtest=LunchSchedulerTest#testSendDailyLunchNotification_Success
```

### 查看測試報告
```bash
# 生成測試報告
mvn surefire-report:report

# 查看報告
open target/site/surefire-report.html
```

---

## 📝 測試最佳實踐

### 1. AAA 模式（Arrange-Act-Assert）

所有測試都遵循 AAA 模式：

```java
@Test
void testExample() {
    // Arrange (Given): 準備測試數據和 mock
    doThrow(new RuntimeException()).when(service).method();

    // Act (When): 執行被測試的方法
    scheduler.sendDailyLunchNotification();

    // Assert (Then): 驗證結果
    verify(service, times(1)).method();
}
```

### 2. 清晰的測試命名

測試方法名稱格式：
```
test[方法名]_[測試場景]
```

例如：
- `testSendDailyLunchNotification_Success`
- `testSendDailyLunchNotification_Exception`

### 3. 使用 @DisplayName

每個測試都有中文描述，便於理解：

```java
@Test
@DisplayName("成功發送午餐通知 - 應該調用 sendPersonalizedLunchNotifications")
void testSendDailyLunchNotification_Success() {
    // ...
}
```

### 4. 測試隔離

- 使用 `@Mock` 隔離依賴
- 每個測試互不影響
- 不依賴外部資源（數據庫、網絡等）

---

## 🐛 常見問題

### Q: 為什麼不測試日誌輸出？

**A**: 日誌輸出是實現細節，不是業務邏輯。我們測試的是：
- 方法是否被正確調用
- 異常是否被正確處理
- 業務邏輯是否正確

### Q: 為什麼不使用 @SpringBootTest？

**A**: 這是**單元測試**，不是整合測試：
- 單元測試：快速、隔離、只測試單一類
- 整合測試：慢速、需要 Spring 容器、測試多個組件協作

對於 `LunchScheduler` 這樣簡單的類，單元測試更適合。

### Q: 如何測試 @Scheduled 是否正確配置？

**A**:
- 單元測試不測試 Spring 註解
- 如需測試定時任務是否觸發，應該寫整合測試
- 可以手動驗證：啟動應用，等待觸發時間

---

## 📚 延伸閱讀

- [JUnit 5 官方文檔](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 官方文檔](https://site.mockito.org/)
- [Spring Boot Testing 指南](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

---

## ✅ 檢查清單

在提交代碼前，確保：

- [x] 所有測試通過 (`mvn test`)
- [x] 測試覆蓋率 100%
- [x] 測試名稱清晰易懂
- [x] 每個測試只測試一個場景
- [x] 使用 AAA 模式組織測試
- [x] Mock 所有外部依賴
- [x] 測試異常處理
- [x] 測試邊界情況

---

**最後更新**: 2025-11-01
**測試狀態**: ✅ 全部通過 (8/8)
