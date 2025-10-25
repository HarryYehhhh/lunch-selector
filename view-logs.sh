#!/bin/bash

# Cloud Run 日誌查看腳本
PROJECT_ID="mercurial-snow-452117-k6"
SERVICE_NAME="lunch-selector"

echo "📋 查看 Cloud Run 日誌..."
echo ""
echo "選擇查看方式："
echo "1. 即時日誌（持續輸出）"
echo "2. 歷史日誌（最近 50 條）"
echo "3. 錯誤日誌（只顯示錯誤）"
echo ""
read -p "請選擇 (1/2/3): " choice

case $choice in
  1)
    echo "📡 即時日誌（按 Ctrl+C 停止）..."
    gcloud logs tail \
      "resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE_NAME" \
      --project=$PROJECT_ID \
      --format="table(timestamp,severity,textPayload)"
    ;;
  2)
    echo "📜 顯示最近 50 條日誌..."
    gcloud logging read \
      "resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE_NAME" \
      --limit=50 \
      --project=$PROJECT_ID \
      --format="table(timestamp,severity,textPayload)"
    ;;
  3)
    echo "❌ 顯示錯誤日誌..."
    gcloud logging read \
      "resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE_NAME AND severity>=ERROR" \
      --limit=50 \
      --project=$PROJECT_ID \
      --format="table(timestamp,severity,textPayload)"
    ;;
  *)
    echo "❌ 無效選擇"
    exit 1
    ;;
esac
