# 🍱 午餐選擇器

自動選擇午餐並透過 LINE 推播的 Spring Boot 應用程式

## 📋 功能特色

- ✅ 隨機選擇午餐餐廳
- ✅ LINE 自動推播通知
- ✅ LINE Webhook 接收訊息（自動取得 User ID）
- ✅ 支援平日定時發送
- ✅ 容器化部署
- ✅ 完全使用 GCP 免費額度

## 🚀 快速開始

### 1. 設定 LINE Messaging API

#### 1.1 建立 LINE Bot

前往 [LINE Developers Console](https://developers.line.biz/console/)：
1. 建立 Provider（如果還沒有）
2. 建立 **Messaging API Channel**
3. 進入 Channel 設定

#### 1.2 取得憑證

**Channel Access Token**（在 Messaging API 頁籤）：
1. 找到 "Channel access token" 區塊
2. 點擊 "Issue" 按鈕產生長效 token
3. 複製產生的 token

**Channel Secret**（在 Basic settings 頁籤）：
1. 找到 "Channel secret"
2. 複製 secret 值

### 2. 設定本地開發環境

#### 2.1 設定 ngrok（用於本地開發）

```bash
# 安裝 ngrok（macOS）
brew install ngrok

# 啟動 ngrok 隧道
ngrok http 8080
```

記下 ngrok 提供的 HTTPS URL（例如：`https://abc123.ngrok-free.app`）

#### 2.2 設定 LINE Webhook

1. 回到 LINE Developers Console > Messaging API 頁籤
2. 找到 "Webhook settings"
3. 設定 **Webhook URL**：`https://YOUR_NGROK_URL/callback`
4. 點擊 "Verify" 驗證連線
5. 開啟 "Use webhook" 開關

### 3. 取得 LINE User ID

1. 在 LINE 中加入你的 Bot 為好友
2. 發送任意訊息給 Bot
3. 查看終端機輸出，會顯示：
   ```
   ════════════════════════════════════════
   📱 收到 LINE 訊息！
   ════════════════════════════════════════
   訊息內容: 你的訊息

   👤 你的 User ID:
   Uxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

   ✅ 請複製上面這個 User ID！
   ════════════════════════════════════════
   ```
4. 複製顯示的 User ID

### 4. 啟動應用程式

**方法 1：使用命令行參數（推薦用於開發）**

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="\
--line.bot.channel-token=YOUR_CHANNEL_TOKEN \
--line.bot.channel-secret=YOUR_CHANNEL_SECRET \
--lunch.user-ids=YOUR_USER_ID"
```

**方法 2：使用環境變數**

```bash
# 設定環境變數
export LINE_CHANNEL_TOKEN="your_token_here"
export LINE_CHANNEL_SECRET="your_secret_here"
export LINE_USER_ID_1="your_user_id_here"

# 執行應用程式
mvn spring-boot:run
```

### 5. 測試 API

```bash
# 健康檢查
curl http://localhost:8080/api/health

# 手動選擇午餐（不發送 LINE）
curl http://localhost:8080/api/lunch/manual

# 發送 LINE 通知
curl -X POST http://localhost:8080/api/lunch/notify
```

成功發送後，你應該會在 LINE 收到午餐通知訊息！

## 🐳 Docker 部署

```bash
# 建構映像檔
docker build -t lunch-selector .

# 執行容器
docker run -p 8080:8080 \
  -e LINE_CHANNEL_TOKEN=xxx \
  -e LINE_CHANNEL_SECRET=xxx \
  -e LINE_USER_ID_1=xxx \
  lunch-selector
```

## ☁️ GCP Cloud Run 部署

### 前置作業

```bash
# 登入 GCP
gcloud auth login

# 設定專案
gcloud config set project YOUR_PROJECT_ID

# 啟用必要的 API
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable cloudscheduler.googleapis.com
```

### 部署步驟

```bash
# 1. 編輯 deploy.sh 填入你的設定
nano deploy.sh

# 2. 執行部署
chmod +x deploy.sh
./deploy.sh
```

### 設定自動排程

```bash
# 取得 Cloud Run URL
SERVICE_URL=$(gcloud run services describe lunch-selector \
  --region asia-east1 \
  --format 'value(status.url)')

# 建立排程任務（平日 11:30 觸發）
gcloud scheduler jobs create http lunch-daily-notify \
  --location=asia-east1 \
  --schedule="30 11 * * 1-5" \
  --time-zone="Asia/Taipei" \
  --uri="$SERVICE_URL/api/lunch/notify" \
  --http-method=POST
```

## 🎯 LINE Bot 設定要點

### Webhook 簽章驗證

應用程式已實作 HMAC-SHA256 簽章驗證，確保只接受來自 LINE 的合法請求：

```java
// WebhookController.java 中的簽章驗證
private boolean validateSignature(String payload, String signature) {
    // 使用 channel secret 計算 HMAC-SHA256
    // 比對 X-Line-Signature header
}
```

這可以防止：
- 未經授權的 webhook 請求
- 請求內容被竄改
- 惡意攻擊者偽造 LINE 請求

### 取得 User ID 的方法

**推薦方法：透過 Webhook（已實作）**
1. 設定好 ngrok 和 webhook
2. 加 Bot 為好友並發送訊息
3. 終端機會自動顯示你的 User ID

**替代方法：LINE 官方工具**
1. 前往 [LINE Developers Console](https://developers.line.biz/console/)
2. 進入你的 Channel > Messaging API 頁籤
3. 點擊 "Your user ID" 查看

## 🍽️ 自訂餐廳清單

編輯 `src/main/resources/restaurants.json` 加入你喜歡的餐廳：

```json
[
  "你的餐廳 1 🍜",
  "你的餐廳 2 🍱",
  "你的餐廳 3 🍔"
]
```

## 📁 專案結構

```
lunch-selector/
├── src/main/
│   ├── java/com/lunch/
│   │   ├── LunchSelectorApplication.java     # 主程式入口
│   │   ├── config/
│   │   │   └── LineConfig.java               # LINE SDK 配置
│   │   ├── controller/
│   │   │   ├── LunchController.java          # REST API 端點
│   │   │   └── WebhookController.java        # LINE Webhook 處理
│   │   └── service/
│   │       ├── LunchService.java             # 午餐選擇邏輯
│   │       └── LineMessagingService.java     # LINE 訊息發送
│   └── resources/
│       ├── application.yml                   # 應用程式配置
│       └── restaurants.json                  # 餐廳清單
├── .gitignore
├── Dockerfile                                # Docker 容器配置
├── deploy.sh                                 # GCP 部署腳本
├── pom.xml                                   # Maven 依賴管理
└── README.md
```

### 核心元件說明

**WebhookController** (`src/main/java/com/lunch/controller/WebhookController.java:28`)
- 處理 LINE webhook 請求（`/callback` endpoint）
- 實作 HMAC-SHA256 簽章驗證
- 解析並顯示收到的訊息及 User ID

**LineMessagingService** (`src/main/java/com/lunch/service/LineMessagingService.java:26`)
- 封裝 LINE Messaging API 調用
- 處理推播訊息邏輯
- 提供詳細的錯誤訊息和日誌

## 💰 成本估算

使用 GCP 免費額度：
- Cloud Run: 每月 200 萬次請求免費
- Artifact Registry: 前 0.5GB 免費
- Cloud Scheduler: 前 3 個任務免費
- LINE Messaging API: 每月 500 則訊息免費

**總計：完全免費！** ✨

## 🔧 常見問題

### Q: LINE 沒收到訊息？

**檢查清單：**
1. **User ID 是否正確**
   - 發送訊息給 Bot，檢查終端機顯示的 User ID
   - 確認啟動參數中的 `--lunch.user-ids` 與顯示的 User ID 一致

2. **是否已加機器人為好友**
   - 必須先在 LINE 中加 Bot 為好友才能收到推播

3. **Channel Access Token 是否有效**
   - 錯誤訊息：`UnauthorizedException: Authentication failed`
   - 解決方法：前往 LINE Console 重新 Issue 新的 token

4. **查看應用程式日誌**
   ```bash
   # 應該看到以下訊息
   ✅ 已成功發送訊息給使用者: Uxxxxx

   # 如果看到錯誤
   ❌ 發送訊息失敗: [錯誤訊息]
   ```

### Q: Webhook 驗證失敗（404 Not Found）？

**可能原因：**
1. ngrok 沒有在運行
2. Spring Boot 應用程式沒有啟動
3. Webhook URL 設定錯誤

**解決方法：**
```bash
# 1. 確認 ngrok 運行中
curl https://YOUR_NGROK_URL/callback

# 2. 確認本地應用運行中
curl http://localhost:8080/callback

# 3. 檢查 webhook URL 格式
# 正確：https://abc123.ngrok-free.app/callback
# 錯誤：http://abc123.ngrok-free.app/callback (少了 s)
```

### Q: 簽章驗證失敗？

確認 `line.bot.channel-secret` 參數正確：
- 從 LINE Console > Basic settings 複製
- 不是 Channel Access Token
- 區分大小寫

### Q: Maven 下載依賴很慢？

使用國內鏡像，編輯 `~/.m2/settings.xml`

### Q: 如何修改推播時間？

修改 Cloud Scheduler 的 `--schedule` 參數（使用 cron 格式）

## 📝 授權

MIT License

## 🙏 致謝

- Spring Boot
- LINE Messaging API
- Google Cloud Platform
