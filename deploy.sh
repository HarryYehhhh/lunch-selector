#!/bin/bash

# GCP Cloud Run 部署腳本
set -e

# ===== 設定區 (請修改這裡) =====
PROJECT_ID="your-gcp-project-id"
REGION="asia-east1"
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

# 建構並推送映像檔
echo "📦 建構 Docker 映像檔..."
gcloud builds submit --tag gcr.io/$PROJECT_ID/$SERVICE_NAME

# 部署到 Cloud Run
echo "☁️  部署到 Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image gcr.io/$PROJECT_ID/$SERVICE_NAME \
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
echo "📍 服務 URL: $SERVICE_URL"
echo ""
echo "🧪 測試指令："
echo "  curl $SERVICE_URL/api/health"
echo "  curl $SERVICE_URL/api/lunch/manual"
echo "  curl -X POST $SERVICE_URL/api/lunch/notify"
echo ""
echo "⏰ 下一步：設定 Cloud Scheduler"
echo "  執行以下指令設定平日 11:30 自動推播："
echo ""
echo "  gcloud scheduler jobs create http lunch-daily-notify \\"
echo "    --location=$REGION \\"
echo "    --schedule=\"30 11 * * 1-5\" \\"
echo "    --time-zone=\"Asia/Taipei\" \\"
echo "    --uri=\"$SERVICE_URL/api/lunch/notify\" \\"
echo "    --http-method=POST"
echo ""
