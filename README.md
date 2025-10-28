# 🍱 午餐選擇器

自動選擇午餐並透過 LINE 推播的 Spring Boot 應用程式

## 📋 功能特色

- ✅ 隨機選擇午餐餐廳
- ✅ LINE 自動推播通知
- ✅ **自動註冊用戶** - 發送訊息或加好友即自動記錄
- ✅ **個性化推薦** - 每個用戶可設定自己的偏好餐廳
- ✅ **智能過濾** - 支援排除不喜歡的餐廳
- ✅ **通知控制** - 用戶可自行開關通知
- ✅ **LINE 互動指令** - 透過 LINE 訊息管理偏好設定
- ✅ LINE Webhook 接收訊息（自動取得 User ID）
- ✅ 支援平日定時發送
- ✅ **Firestore 雲端存儲** - 持久化用戶數據和偏好設定
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

### 3. 設定 Firestore（本地開發）

#### 3.1 創建 GCP 專案和 Firestore

```bash
# 1. 前往 Google Cloud Console
https://console.cloud.google.com/

# 2. 創建新專案或選擇現有專案

# 3. 啟用 Firestore API
gcloud services enable firestore.googleapis.com

# 4. 創建 Firestore 資料庫
# 前往 Firestore > 創建資料庫
# 選擇「Native 模式」
# 選擇位置：asia-east1 (台灣)
```

#### 3.2 設定服務帳號金鑰（本地開發用）

```bash
# 1. 前往 IAM & Admin > Service Accounts
# 2. 創建服務帳號（或使用現有的）
# 3. 賦予角色：Cloud Datastore User
# 4. 創建金鑰（JSON 格式）
# 5. 下載金鑰文件到專案目錄，例如：firestore-key.json

# 6. 設定環境變數
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/firestore-key.json"
export GCP_PROJECT_ID="your-project-id"
```

**注意：**
- ⚠️ 金鑰文件包含敏感資訊，不要提交到 Git
- ⚠️ 已在 `.gitignore` 中排除 `*-key.json`

### 4. 啟動應用程式

**方法 1：使用命令行參數**

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="\
--line.bot.channel-token=YOUR_CHANNEL_TOKEN \
--line.bot.channel-secret=YOUR_CHANNEL_SECRET \
--gcp.project-id=YOUR_PROJECT_ID"
```

**方法 2：使用環境變數（推薦）**

```bash
# 設定環境變數
export LINE_CHANNEL_TOKEN="your_token_here"
export LINE_CHANNEL_SECRET="your_secret_here"
export GCP_PROJECT_ID="your-project-id"
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/firestore-key.json"

# 執行應用程式
mvn spring-boot:run
```

### 5. 使用 LINE Bot

#### 5.1 註冊用戶（自動）

1. **在 LINE 中加入你的 Bot 為好友**
   - 系統會自動在 Firestore 註冊你的 User ID

2. **發送 "幫助" 查看所有指令**
   ```
   📖 午餐選擇器使用說明

   🍽️ 基本指令：
   • 推薦 / 午餐 / 吃什麼 - 立即獲得推薦
   • 查看餐廳 - 查看所有可用餐廳
   • 查看偏好 - 查看你的偏好設定

   ⚙️ 設定指令：
   • 設定 餐廳1,餐廳2,餐廳3 - 只從這些餐廳中推薦
   • 排除 餐廳1,餐廳2 - 不推薦這些餐廳
   • 清除偏好 - 清除所有設定

   🔔 通知控制：
   • 開啟通知 - 啟用定時通知
   • 關閉通知 - 停止定時通知
   ```

#### 5.2 設定個人偏好

**設定偏好餐廳：**
```
你：設定 麥當勞,肯德基,便當店

Bot：✅ 已設定偏好餐廳：

麥當勞
肯德基
便當店

今後只會從這些餐廳中推薦
```

**排除不喜歡的餐廳：**
```
你：排除 壽司,火鍋

Bot：✅ 已設定排除餐廳：

壽司
火鍋

這些餐廳不會被推薦
```

**查看目前設定：**
```
你：查看偏好

Bot：⚙️ 你的偏好設定：

🔔 通知狀態：已開啟

✅ 偏好餐廳：
  • 麥當勞
  • 肯德基
  • 便當店

❌ 排除餐廳：
  • 壽司
  • 火鍋
```

**立即獲得推薦：**
```
你：推薦

Bot：🍽️ 今日午餐建議 🍽️

📅 日期：2025/10/27 (日)
🎯 推薦：麥當勞 🍔

祝用餐愉快！😋
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

# 執行容器（使用 volume 保存用戶數據）
docker run -p 8080:8080 \
  -v $(pwd)/users.json:/app/users.json \
  -e LINE_CHANNEL_TOKEN=xxx \
  -e LINE_CHANNEL_SECRET=xxx \
  lunch-selector
```

**注意：** 使用 `-v` 參數掛載 `users.json` 可確保用戶數據在容器重啟後仍然保留

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
│   │   ├── scheduler/
│   │   │   ├── LunchScheduler.java           # 平日定時發送
│   │   │   └── TestScheduler.java            # 測試用排程
│   │   └── service/
│   │       ├── LunchService.java             # 午餐選擇邏輯
│   │       ├── LineMessagingService.java     # LINE 訊息發送
│   │       └── UserStorageService.java       # 🆕 用戶存儲管理
│   └── resources/
│       ├── application.yml                   # 應用程式配置
│       └── restaurants.json                  # 餐廳清單
├── users.json                                # 🆕 用戶 ID 存儲文件
├── .gitignore
├── Dockerfile                                # Docker 容器配置
├── deploy.sh                                 # GCP 部署腳本
├── setup.sh                                  # 環境設置腳本
├── pom.xml                                   # Maven 依賴管理
└── README.md
```

### 核心元件說明

**WebhookController** (`src/main/java/com/lunch/controller/WebhookController.java:31`)
- 處理 LINE webhook 請求（`/callback` endpoint）
- 實作 HMAC-SHA256 簽章驗證
- 解析並顯示收到的訊息及 User ID
- **自動註冊新用戶**（follow 和 message 事件）
- 處理取消好友事件（自動移除用戶）

**LineMessagingService** (`src/main/java/com/lunch/service/LineMessagingService.java:23`)
- 封裝 LINE Messaging API 調用
- **從 UserStorageService 動態獲取用戶列表**
- 批量發送推播訊息
- 提供詳細的錯誤訊息和統計日誌

**UserStorageService** (`src/main/java/com/lunch/service/UserStorageService.java:30`) 🆕
- 管理用戶 ID 的持久化存儲
- 使用 JSON 文件存儲（`users.json`）
- 提供線程安全的讀寫操作
- 自動去重和檔案管理
- 支援手動重新載入

## 💰 成本估算

使用 GCP 免費額度：
- **Cloud Run**: 每月 200 萬次請求免費
- **Firestore**:
  - 存儲：1GB 免費
  - 讀取：50,000 次/天免費
  - 寫入：20,000 次/天免費
  - 刪除：20,000 次/天免費
- **Artifact Registry**: 前 0.5GB 免費
- **Cloud Scheduler**: 前 3 個任務免費
- **LINE Messaging API**: 每月 500 則訊息免費

**實際用量估算（50 個用戶）：**
- Firestore 存儲：< 1MB（遠低於 1GB 免費額度）
- Firestore 讀取：每天 ~100 次（遠低於 50,000 次）
- Firestore 寫入：每天 ~50 次（遠低於 20,000 次）
- LINE 訊息：每月 ~1000 則（超過免費額度後每則 ~$0.0003）

**總計：幾乎完全免費！** ✨
（除非用戶數超過 500 人）

## 🔧 常見問題

### Q: LINE 沒收到訊息？

**檢查清單：**
1. **確認用戶已註冊**
   - 檢查 `users.json` 文件中是否有你的 User ID
   - 或查看應用程式啟動時的日誌：
     ```
     ✅ 用戶存儲服務初始化成功，已載入 X 位用戶
     ```

2. **是否已加機器人為好友**
   - 必須先在 LINE 中加 Bot 為好友才能收到推播
   - 加好友或發送訊息後，系統會自動註冊

3. **Channel Access Token 是否有效**
   - 錯誤訊息：`UnauthorizedException: Authentication failed`
   - 解決方法：前往 LINE Console 重新 Issue 新的 token

4. **查看應用程式日誌**
   ```bash
   # 應該看到以下訊息
   準備發送通知給 X 位用戶
   ✅ 已成功發送訊息給使用者: Uxxxxx
   📊 通知發送完成 - 成功: X, 失敗: 0, 總計: X

   # 如果看到錯誤
   ⚠️ 沒有註冊的用戶！
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

### Q: 如何管理用戶？

**透過 LINE 指令：**
```
• 開啟通知 / 關閉通知 - 控制是否接收推薦
• 查看偏好 - 查看自己的設定
• 清除偏好 - 重置所有設定
```

**透過 Firestore Console：**
1. 前往 [Firestore Console](https://console.cloud.google.com/firestore)
2. 選擇 `users` Collection
3. 可以查看、編輯、刪除用戶資料

**查看所有用戶：**
```bash
# 使用 gcloud CLI
gcloud firestore collections list

# 查詢特定用戶
gcloud firestore documents describe projects/YOUR_PROJECT/databases/(default)/documents/users/USER_ID
```

**移除用戶：**
- 用戶取消好友時會自動設為非活躍（`active: false`）
- 不會真的刪除數據（保留歷史記錄）

### Q: Firestore 數據結構是什麼？

```
Collection: users
  Document ID: LINE_USER_ID
    ├── userId: string
    ├── registeredAt: timestamp
    ├── lastActiveAt: timestamp
    ├── active: boolean
    └── preferences: map
        ├── restaurants: array<string>
        ├── excludeRestaurants: array<string>
        ├── notificationEnabled: boolean
        └── customMessage: string
```

**查詢範例：**
```bash
# 查看所有啟用通知的用戶
gcloud firestore documents list \
  projects/YOUR_PROJECT/databases/(default)/documents/users \
  --filter="active=true AND preferences.notificationEnabled=true"
```

### Q: Firestore 會不會有費用？

**免費額度（每天）：**
- 存儲：1GB
- 讀取：50,000 次
- 寫入：20,000 次
- 刪除：20,000 次

**實際用量（50 個用戶）：**
- 存儲：< 1MB ✅
- 讀取：~100 次/天 ✅
- 寫入：~50 次/天 ✅

**結論：完全在免費額度內！** 🎉

## 📝 授權

MIT License

## 🙏 致謝

- Spring Boot
- LINE Messaging API
- Google Cloud Platform
