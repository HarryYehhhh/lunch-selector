#!/bin/bash

# GCP 初始設定腳本
set -e

echo "🔧 GCP 初始設定"
echo ""

# 檢查 PROJECT_ID
read -p "請輸入你的 GCP Project ID: " PROJECT_ID

if [ -z "$PROJECT_ID" ]; then
    echo "❌ Project ID 不能為空"
    exit 1
fi

echo ""
echo "📋 設定專案: $PROJECT_ID"
gcloud config set project $PROJECT_ID

echo ""
echo "🔌 啟用必要的 API..."

# 啟用 Cloud Run API
gcloud services enable run.googleapis.com

# 啟用 Cloud Build API
gcloud services enable cloudbuild.googleapis.com

# 啟用 Container Registry API
gcloud services enable containerregistry.googleapis.com

# 啟用 Cloud Scheduler API (如果需要額外的定時任務)
gcloud services enable cloudscheduler.googleapis.com

# 啟用 App Engine Admin API (Cloud Scheduler 需要)
gcloud services enable appengine.googleapis.com

echo ""
echo "✅ API 啟用完成！"
echo ""
echo "📝 下一步："
echo "1. 確認 .env 檔案中有正確的 LINE 憑證"
echo "2. 編輯 deploy.sh，將 PROJECT_ID 改為: $PROJECT_ID"
echo "3. 執行: ./deploy.sh"
echo ""
