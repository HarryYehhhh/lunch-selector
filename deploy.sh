#!/bin/bash

# GCP Cloud Run 部署腳本
set -e

# ===== 設定區 (請修改這裡) =====
PROJECT_ID="mercurial-snow-452117-k6"
REGION="asia-east1"
REPOSITORY_NAME="lunch-selector-repo"
SERVICE_NAME="lunch-selector"
# ================================

echo "🚀 開始部署到 GCP Cloud Run..."
echo ""

# 檢查是否已設定環境變數
if [ ! -f .env ]; then
    echo "❌ 找不到 .env 檔案"
    echo "請先執行: cp .env.example .env"
    echo "並填入你的 LINE 設定"
    exit 1
fi

# 載入環境變數
source .env

# 檢查必要變數
if [ -z "$LINE_CHANNEL_TOKEN" ] || [ -z "$LINE_CHANNEL_SECRET" ] || [ -z "$LINE_USER_ID_1" ]; then
    echo "❌ 請在 .env 中設定所有必要的環境變數"
    exit 1
fi

# 確認 PROJECT_ID 已修改
if [ "$PROJECT_ID" = "your-gcp-project-id" ]; then
    echo "❌ 請先在 deploy.sh 中修改 PROJECT_ID"
    exit 1
fi

# 設定 GCP 專案
echo "📋 設定 GCP 專案: $PROJECT_ID"
gcloud config set project $PROJECT_ID

# 檢查並建立 Artifact Registry repository
echo "🔍 檢查 Artifact Registry repository..."
if ! gcloud artifacts repositories describe $REPOSITORY_NAME \
  --location=$REGION \
  --project=$PROJECT_ID &>/dev/null; then
    echo "📦 建立 Artifact Registry repository: $REPOSITORY_NAME"
    gcloud artifacts repositories create $REPOSITORY_NAME \
      --repository-format=docker \
      --location=$REGION \
      --description="Lunch Selector Docker images" \
      --project=$PROJECT_ID
    echo "✅ Repository 建立完成"
else
    echo "✅ Repository 已存在"
fi

# 設定映像檔完整路徑
IMAGE_PATH="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY_NAME/$SERVICE_NAME"

# 建構並推送映像檔到 Artifact Registry
echo "📦 建構 Docker 映像檔並推送到 Artifact Registry..."
echo "   映像位置: $IMAGE_PATH"
gcloud builds submit --tag $IMAGE_PATH

# 部署到 Cloud Run
echo "☁️  部署到 Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image $IMAGE_PATH \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --set-env-vars LINE_CHANNEL_TOKEN=$LINE_CHANNEL_TOKEN \
  --set-env-vars LINE_CHANNEL_SECRET=$LINE_CHANNEL_SECRET \
  --set-env-vars LINE_USER_ID_1=$LINE_USER_ID_1 \
  --memory 512Mi \
  --cpu 1

# 取得服務 URL
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME \
  --region $REGION \
  --format 'value(status.url)')

echo ""
echo "✅ 部署完成！"
echo ""
echo "📍 服務資訊："
echo "  URL: $SERVICE_URL"
echo "  映像: $IMAGE_PATH"
echo "  區域: $REGION"
echo ""
echo "🧪 測試指令："
echo "  curl $SERVICE_URL/api/health"
echo "  curl $SERVICE_URL/api/lunch/manual"
echo "  curl -X POST $SERVICE_URL/api/lunch/notify"
echo ""
echo "⏰ 定時任務說明："
echo "  ✅ 應用程式已內建定時任務功能"
echo "  📅 每個平日 11:50 自動發送午餐推薦"
echo "  🔧 位置: src/main/java/com/lunch/scheduler/LunchScheduler.java"
echo ""
echo "💡 提示："
echo "  - 使用 Artifact Registry 儲存映像檔（推薦方式）"
echo "  - Cloud Run 會自動保持服務運行來執行定時任務"
echo "  - 如需修改時間，請編輯 LunchScheduler.java 中的 cron 表達式"
echo ""
echo "🔗 更新 LINE Webhook URL:"
echo "  前往 LINE Developers Console 設定 Webhook URL:"
echo "  $SERVICE_URL/callback"
echo ""
echo "📦 Artifact Registry 資訊："
echo "  Repository: $REPOSITORY_NAME"
echo "  Location: $REGION"
echo "  查看映像: gcloud artifacts docker images list $REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY_NAME"
echo ""
