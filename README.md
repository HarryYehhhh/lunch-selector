# 🍱 午餐選擇器

自動選擇午餐並透過 LINE 推播的 Spring Boot 應用程式

## 📋 功能特色

- ✅ 隨機選擇午餐餐廳
- ✅ LINE 自動推播通知
- ✅ 支援平日定時發送
- ✅ 容器化部署
- ✅ 完全使用 GCP 免費額度

## 🚀 快速開始

### 1. 設定 LINE Messaging API

前往 [LINE Developers Console](https://developers.line.biz/console/)：
1. 建立 Provider
2. 建立 Messaging API Channel
3. 取得 Channel Access Token 和 Channel Secret
4. 加機器人為好友並取得 User ID

### 2. 設定環境變數

```bash
# 複製範本
cp .env.example .env

# 編輯 .env 填入你的 LINE 設定
nano .env
```

### 3. 執行應用程式

```bash
# 載入環境變數並執行
export $(cat .env | xargs)
mvn spring-boot:run
```

### 4. 測試 API

```bash
# 健康檢查
curl http://localhost:8080/api/health

# 手動選擇午餐（不發送 LINE）
curl http://localhost:8080/api/lunch/manual

# 發送 LINE 通知
curl -X POST http://localhost:8080/api/lunch/notify
```

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

## 🎯 取得 LINE User ID

### 方法 1: 查看應用程式 Log

1. 加機器人為好友
2. 發送任意訊息
3. 查看應用程式 log 會顯示 User ID

### 方法 2: 使用 LINE 官方工具

1. 前往 [LINE Developers Console](https://developers.line.biz/console/)
2. 進入你的 Channel
3. 在 Messaging API 頁籤點擊「Your user ID」

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
│   │   ├── LunchSelectorApplication.java
│   │   ├── controller/
│   │   │   └── LunchController.java
│   │   └── service/
│   │       ├── LunchService.java
│   │       └── LineMessagingService.java
│   └── resources/
│       ├── application.yml
│       └── restaurants.json
├── .env (需自行建立)
├── .env.example
├── .gitignore
├── Dockerfile
├── deploy.sh
├── pom.xml
└── README.md
```

## 💰 成本估算

使用 GCP 免費額度：
- Cloud Run: 每月 200 萬次請求免費
- Artifact Registry: 前 0.5GB 免費
- Cloud Scheduler: 前 3 個任務免費
- LINE Messaging API: 每月 500 則訊息免費

**總計：完全免費！** ✨

## 🔧 常見問題

### Q: LINE 沒收到訊息？
檢查：
1. User ID 是否正確
2. 是否已加機器人為好友
3. Channel Access Token 是否有效
4. 查看應用程式 log 的錯誤訊息

### Q: Maven 下載依賴很慢？
使用國內鏡像，編輯 `~/.m2/settings.xml`

### Q: 如何修改推播時間？
修改 Cloud Scheduler 的 `--schedule` 參數

## 📝 授權

MIT License

## 🙏 致謝

- Spring Boot
- LINE Messaging API
- Google Cloud Platform
