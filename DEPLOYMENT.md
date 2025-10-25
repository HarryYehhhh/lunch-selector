# 🚀 部署指南

本專案已完整整合 Artifact Registry 部署流程。

## 📦 使用的 GCP 服務

### 主要服務
1. **Cloud Run** - 運行應用程式容器
2. **Artifact Registry** - 儲存 Docker 映像檔（推薦方式）
3. **Cloud Build** - 建構 Docker 映像檔
4. **Cloud Storage** - 儲存建構紀錄
5. **Cloud Logging** - 應用程式日誌

### 為什麼使用 Artifact Registry？

✅ **官方推薦** - Google Cloud 推薦的映像儲存方案
✅ **現代化** - 支援多種格式（Docker, Maven, npm, Python 等）
✅ **安全性** - 更細緻的權限控制和安全掃描
✅ **未來保障** - Container Registry 將在 2025-2026 年淘汰

對比舊的 Container Registry:
- ❌ Container Registry (`gcr.io`) - 即將淘汰
- ✅ Artifact Registry (`region-docker.pkg.dev`) - 現在使用

## 🔧 部署流程

### 一鍵部署

```bash
./deploy.sh
```

### 部署腳本會自動執行以下步驟：

1. **檢查環境變數** - 確認 `.env` 檔案存在且設定正確
2. **檢查 Artifact Registry Repository** - 如不存在則自動建立
3. **建構 Docker 映像** - 使用 Cloud Build
4. **推送到 Artifact Registry** - 儲存映像檔
5. **部署到 Cloud Run** - 啟動服務
6. **顯示服務資訊** - URL、映像位置等

## 📝 配置參數

在 `deploy.sh` 中可調整：

```bash
PROJECT_ID="mercurial-snow-452117-k6"      # GCP 專案 ID
REGION="asia-east1"                        # 部署區域
REPOSITORY_NAME="lunch-selector-repo"      # Artifact Registry repository 名稱
SERVICE_NAME="lunch-selector"              # Cloud Run 服務名稱
```

## 🔍 查看部署資源

### 查看 Cloud Run 服務
```bash
gcloud run services list --project=mercurial-snow-452117-k6
```

### 查看 Artifact Registry 映像
```bash
gcloud artifacts docker images list \
  asia-east1-docker.pkg.dev/mercurial-snow-452117-k6/lunch-selector-repo
```

### 查看 Artifact Registry Repository
```bash
gcloud artifacts repositories list \
  --location=asia-east1 \
  --project=mercurial-snow-452117-k6
```

### 查看映像版本歷史
```bash
gcloud artifacts docker images list \
  asia-east1-docker.pkg.dev/mercurial-snow-452117-k6/lunch-selector-repo/lunch-selector \
  --include-tags
```

## 🗑️ 清理資源

### 刪除 Cloud Run 服務
```bash
gcloud run services delete lunch-selector \
  --region=asia-east1 \
  --project=mercurial-snow-452117-k6
```

### 刪除 Artifact Registry Repository
```bash
gcloud artifacts repositories delete lunch-selector-repo \
  --location=asia-east1 \
  --project=mercurial-snow-452117-k6
```

### 刪除特定映像版本
```bash
gcloud artifacts docker images delete \
  asia-east1-docker.pkg.dev/mercurial-snow-452117-k6/lunch-selector-repo/lunch-selector:TAG
```

## 📊 映像檔位置

**完整路徑格式**:
```
asia-east1-docker.pkg.dev/mercurial-snow-452117-k6/lunch-selector-repo/lunch-selector
```

**格式說明**:
```
[REGION]-docker.pkg.dev/[PROJECT_ID]/[REPOSITORY]/[IMAGE_NAME]
```

## 🔄 更新部署

修改代碼後重新部署：

```bash
# 1. 提交代碼變更（如果使用 git）
git add .
git commit -m "Update feature"

# 2. 重新部署
./deploy.sh
```

Cloud Build 會自動：
- 建構新的映像檔
- 使用最新的 tag
- 更新 Cloud Run 服務

## ⏰ 內建定時任務

應用程式內建定時任務功能：
- **執行時間**: 每個平日 11:50
- **功能**: 自動發送午餐推薦到 LINE
- **配置檔**: `src/main/java/com/lunch/scheduler/LunchScheduler.java`

無需額外設定 Cloud Scheduler！

## 💰 成本估算

基於目前配置，**完全在 GCP 免費額度內**：

| 服務 | 免費額度 | 預估使用量 | 費用 |
|------|---------|-----------|------|
| Cloud Run | 200 萬次請求/月 | ~1,000 次/月 | $0 |
| Artifact Registry | 0.5GB 儲存 | ~100MB | $0 |
| Cloud Build | 120 分鐘/天 | ~5 分鐘/天 | $0 |
| Cloud Storage | 5GB | <1GB | $0 |
| Cloud Logging | 50GB/月 | <1GB/月 | $0 |
| **總計** | - | - | **$0** ✨ |

## 🔗 相關文檔

- [Cloud Run 文檔](https://cloud.google.com/run/docs)
- [Artifact Registry 文檔](https://cloud.google.com/artifact-registry/docs)
- [Cloud Build 文檔](https://cloud.google.com/build/docs)
- [gcloud CLI 參考](https://cloud.google.com/sdk/gcloud/reference)

## 📚 其他腳本

- `./setup-gcp.sh` - 初始化 GCP 專案並啟用必要的 API
- `./view-logs.sh` - 查看 Cloud Run 日誌
- `./deploy.sh` - 部署應用程式（包含 Artifact Registry 整合）

## ✅ 檢查清單

部署前確認：
- [ ] 已安裝 gcloud CLI
- [ ] 已執行 `gcloud init` 並登入
- [ ] 已建立 GCP 專案
- [ ] 已執行 `./setup-gcp.sh` 啟用 API
- [ ] `.env` 檔案包含正確的 LINE 憑證
- [ ] `deploy.sh` 中的 `PROJECT_ID` 已修改

部署後驗證：
- [ ] Cloud Run 服務正常運行
- [ ] 健康檢查端點回應 OK
- [ ] LINE Webhook 設定並驗證成功
- [ ] 定時任務按時執行（檢查日誌）
