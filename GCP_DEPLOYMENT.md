# ☁️ GCP Cloud Run 部署完整指南

## 📋 目錄
- [前置準備](#前置準備)
- [Container Registry vs Artifact Registry](#container-registry-vs-artifact-registry)
- [初次部署](#初次部署)
- [更新部署](#更新部署)
- [gcloud CLI 命令速查表](#gcloud-cli-命令速查表)
- [環境管理](#環境管理)
- [故障排除](#故障排除)
- [成本優化](#成本優化)
- [GCP 服務說明](#gcp-服務說明)
- [完整部署腳本](#完整部署腳本)
- [監控和告警](#監控和告警)
- [參考資源](#參考資源)

---

## 前置準備

### 1. 安裝 Google Cloud SDK

**macOS**:
```bash
# 使用 Homebrew 安裝
brew install --cask google-cloud-sdk

# 或使用官方安裝腳本
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
```

**驗證安裝**:
```bash
gcloud --version
```

**預期輸出**:
```
Google Cloud SDK 450.0.0
bq 2.0.98
core 2023.11.03
gcloud-crc32c 1.0.0
gsutil 5.27
```

---

### 2. 設置 GCP 專案

#### 登入 Google Cloud
```bash
gcloud auth login
```

這會打開瀏覽器進行身份驗證。

#### 設置專案 ID
```bash
# 查看所有專案
gcloud projects list

# 設置當前專案
export GCP_PROJECT_ID="mercurial-snow-452117-k6"
gcloud config set project $GCP_PROJECT_ID

# 驗證設置
gcloud config get-value project
```

#### 啟用必要的 API
```bash
# 啟用 Cloud Run API
gcloud services enable run.googleapis.com

# 啟用 Container Registry API
gcloud services enable containerregistry.googleapis.com

# 啟用 Artifact Registry API（推薦用於新專案）
gcloud services enable artifactregistry.googleapis.com

# 啟用 Cloud Build API（如果使用 Cloud Build）
gcloud services enable cloudbuild.googleapis.com

# 啟用 Secret Manager API
gcloud services enable secretmanager.googleapis.com

# 驗證已啟用的服務
gcloud services list --enabled
```

---

### 3. 配置 Docker 認證

#### 使用 Container Registry (GCR)
```bash
# 配置 Docker 認證
gcloud auth configure-docker

# 或指定特定區域
gcloud auth configure-docker gcr.io
```

#### 使用 Artifact Registry（推薦）
```bash
# 創建 Artifact Registry repository（僅需一次）
gcloud artifacts repositories create lunch-selector \
  --repository-format=docker \
  --location=asia-east1 \
  --description="Lunch Selector Docker images"

# 配置 Docker 認證
gcloud auth configure-docker asia-east1-docker.pkg.dev
```

---

### 4. 設置 Firestore 和 Secret Manager

#### 確認 Firestore 已啟用
```bash
# 在 GCP Console 檢查
# https://console.cloud.google.com/firestore

# 或使用 CLI
gcloud firestore databases list
```

#### 將敏感資訊存入 Secret Manager
```bash
# 啟用 Secret Manager API
gcloud services enable secretmanager.googleapis.com

# 創建 LINE Channel Token secret
echo -n "YOUR_LINE_CHANNEL_TOKEN" | \
  gcloud secrets create line-channel-token \
  --data-file=-

# 創建 LINE Channel Secret
echo -n "YOUR_LINE_CHANNEL_SECRET" | \
  gcloud secrets create line-channel-secret \
  --data-file=-

# 驗證 secrets
gcloud secrets list

# 查看 secret 版本
gcloud secrets versions list line-channel-token
```

---

## Container Registry vs Artifact Registry

### 為什麼需要 Repository 層級？

#### Container Registry 的局限
```
gcr.io/my-company/
├── frontend-app
├── backend-api
├── internal-tool
├── third-party-service
└── experimental-project
```

所有映像都在同一層級，無法：
- ❌ 對不同映像群組設定不同權限
- ❌ 按團隊或環境分組管理
- ❌ 設定不同的掃描政策
- ❌ 混合不同類型的 artifacts（Docker + Maven）

---

### Artifact Registry 的解決方案

#### Repository 層級隔離
```
asia-east1-docker.pkg.dev/my-company/
├── production-apps/           ← Repository 1 (生產環境)
│   ├── frontend-app
│   └── backend-api
├── development-apps/          ← Repository 2 (開發環境)
│   ├── frontend-app-dev
│   └── backend-api-dev
├── team-a-projects/          ← Repository 3 (A 團隊)
│   ├── service-x
│   └── service-y
└── third-party/              ← Repository 4 (第三方)
    └── nginx
```

---

### 實際應用場景

#### 場景 1: 環境隔離
```bash
# 生產環境 repository - 嚴格權限控制
asia-east1-docker.pkg.dev/PROJECT/production/lunch-selector

# 開發環境 repository - 寬鬆權限
asia-east1-docker.pkg.dev/PROJECT/development/lunch-selector-dev

# 測試環境 repository
asia-east1-docker.pkg.dev/PROJECT/staging/lunch-selector-staging
```

**權限設定**:
- `production`: 只有 DevOps 團隊可以推送
- `development`: 所有開發者可以推送
- `staging`: QA 團隊可以推送

#### 場景 2: 多種 Artifact 類型
```bash
# Docker 映像 repository
asia-east1-docker.pkg.dev/PROJECT/docker-images/lunch-selector

# Maven 套件 repository
asia-east1-maven.pkg.dev/PROJECT/java-libraries/my-lib

# npm 套件 repository
asia-east1-npm.pkg.dev/PROJECT/npm-packages/my-package

# Python 套件 repository
asia-east1-python.pkg.dev/PROJECT/python-packages/my-module
```

---

### 對比總結

| 特性 | Container Registry | Artifact Registry |
|------|-------------------|-------------------|
| **隔離層級** | 專案 | 專案 → Repository → 映像 |
| **權限控制** | 專案層級 | Repository 層級 ⭐ |
| **組織方式** | 扁平 | 分層（可按團隊/環境/用途）⭐ |
| **掃描政策** | 統一 | 每個 repository 獨立 ⭐ |
| **清理政策** | 統一 | 每個 repository 獨立 ⭐ |
| **多租戶** | 困難 | 容易 ⭐ |
| **支援格式** | Docker only | Docker, Maven, npm, Python, Go... ⭐ |
| **狀態** | 將於 2025-2026 年淘汰 ❌ | 官方推薦 ✅ |

### 推薦選擇

- ✅ **新專案**: 使用 Artifact Registry
- ⚠️ **現有專案**: 逐步遷移到 Artifact Registry
- ❌ **避免**: 不要開始使用 Container Registry

---

## 初次部署

### 步驟 1: 編譯專案

```bash
# 清理並編譯
mvn clean package -DskipTests

# 驗證 JAR 檔案
ls -lh target/lunch-selector-1.0.0.jar
```

**預期輸出**:
```
-rw-r--r--  1 user  staff   50M  Nov 12 10:00 target/lunch-selector-1.0.0.jar
```

---

### 步驟 2: 構建 Docker 映像

```bash
# 設置專案 ID
export GCP_PROJECT_ID="mercurial-snow-452117-k6"

# 構建映像（不使用快取）
docker build --no-cache -t lunch-selector:latest .

# 為 GCR 添加標籤
docker tag lunch-selector:latest gcr.io/$GCP_PROJECT_ID/lunch-selector:latest

# 或為 Artifact Registry 添加標籤（推薦）
docker tag lunch-selector:latest asia-east1-docker.pkg.dev/$GCP_PROJECT_ID/lunch-selector/app:latest
```

---

### 步驟 3: 推送映像到 GCP

#### 使用 Container Registry (GCR)
```bash
# 推送映像
docker push gcr.io/$GCP_PROJECT_ID/lunch-selector:latest

# 驗證推送
gcloud container images list --repository=gcr.io/$GCP_PROJECT_ID
gcloud container images describe gcr.io/$GCP_PROJECT_ID/lunch-selector:latest
```

#### 使用 Artifact Registry（推薦）
```bash
# 推送映像
docker push asia-east1-docker.pkg.dev/$GCP_PROJECT_ID/lunch-selector/app:latest

# 驗證推送
gcloud artifacts docker images list asia-east1-docker.pkg.dev/$GCP_PROJECT_ID/lunch-selector

# 查看映像詳情
gcloud artifacts docker images describe \
  asia-east1-docker.pkg.dev/$GCP_PROJECT_ID/lunch-selector/app:latest
```

---

### 步驟 4: 部署到 Cloud Run

#### 基本部署
```bash
gcloud run deploy lunch-selector \
  --image gcr.io/$GCP_PROJECT_ID/lunch-selector:latest \
  --platform managed \
  --region asia-east1 \
  --allow-unauthenticated \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID \
  --set-env-vars GOOGLE_APPLICATION_CREDENTIALS=/app/firestore-key.json
```

#### 完整部署（帶 Secrets 和資源限制）⭐ 推薦
```bash
gcloud run deploy lunch-selector \
  --image gcr.io/$GCP_PROJECT_ID/lunch-selector:latest \
  --platform managed \
  --region asia-east1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --timeout 300 \
  --max-instances 10 \
  --min-instances 0 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID \
  --set-secrets LINE_CHANNEL_TOKEN=line-channel-token:latest \
  --set-secrets LINE_CHANNEL_SECRET=line-channel-secret:latest \
  --service-account lunch-selector-sa@$GCP_PROJECT_ID.iam.gserviceaccount.com
```

**參數說明**:
- `--image`: Docker 映像位置
- `--platform managed`: 使用 Cloud Run 託管平台
- `--region asia-east1`: 部署區域（台灣）
- `--allow-unauthenticated`: 允許公開訪問
- `--memory 512Mi`: 記憶體限制
- `--cpu 1`: CPU 限制
- `--timeout 300`: 請求超時時間（秒）
- `--max-instances 10`: 最大實例數
- `--min-instances 0`: 最小實例數（0 = scale to zero）
- `--set-env-vars`: 設置環境變數
- `--set-secrets`: 從 Secret Manager 載入敏感資訊

---

### 步驟 5: 驗證部署

```bash
# 取得服務 URL
export SERVICE_URL=$(gcloud run services describe lunch-selector \
  --region asia-east1 \
  --format 'value(status.url)')

echo "Service URL: $SERVICE_URL"

# 測試健康檢查
curl $SERVICE_URL

# 測試 API
curl $SERVICE_URL/api/users/TEST_USER_001/restaurants

# 查看服務狀態
gcloud run services describe lunch-selector --region asia-east1
```

---

## 更新部署

### 快速更新流程

```bash
# 1. 設置變數
export GCP_PROJECT_ID="mercurial-snow-452117-k6"

# 2. 編譯
mvn clean package -DskipTests

# 3. 構建並推送（使用新標籤）
VERSION=$(date +%Y%m%d-%H%M%S)
docker build --no-cache -t lunch-selector:$VERSION .
docker tag lunch-selector:$VERSION gcr.io/$GCP_PROJECT_ID/lunch-selector:$VERSION
docker tag lunch-selector:$VERSION gcr.io/$GCP_PROJECT_ID/lunch-selector:latest
docker push gcr.io/$GCP_PROJECT_ID/lunch-selector:$VERSION
docker push gcr.io/$GCP_PROJECT_ID/lunch-selector:latest

# 4. 部署更新
gcloud run deploy lunch-selector \
  --image gcr.io/$GCP_PROJECT_ID/lunch-selector:latest \
  --region asia-east1

# 5. 驗證
curl $(gcloud run services describe lunch-selector --region asia-east1 --format 'value(status.url)')/api/users/TEST_USER_001/restaurants
```

---

### 金絲雀部署（逐步推出）

```bash
# 部署新版本到 50% 的流量
gcloud run services update-traffic lunch-selector \
  --to-revisions LATEST=50 \
  --region asia-east1

# 驗證無誤後，完全切換到新版本
gcloud run services update-traffic lunch-selector \
  --to-latest \
  --region asia-east1

# 或回滾到舊版本
gcloud run services update-traffic lunch-selector \
  --to-revisions lunch-selector-00001-abc=100 \
  --region asia-east1
```

---

## gcloud CLI 命令速查表

### 基本設定

```bash
# 登入 GCP
gcloud auth login

# 初始化設定
gcloud init

# 查看當前設定
gcloud config list

# 設定專案
gcloud config set project PROJECT_ID

# 設定預設區域
gcloud config set run/region asia-east1

# 查看所有專案
gcloud projects list

# 查看所有可用區域
gcloud compute regions list

# 查看 Cloud Run 可用區域
gcloud run regions list
```

---

### Cloud Run 指令

#### 部署服務
```bash
# 基本部署
gcloud run deploy SERVICE_NAME \
  --image gcr.io/PROJECT_ID/IMAGE_NAME \
  --region REGION

# 完整部署（含環境變數）
gcloud run deploy lunch-selector \
  --image gcr.io/PROJECT_ID/lunch-selector \
  --platform managed \
  --region asia-east1 \
  --allow-unauthenticated \
  --set-env-vars KEY1=VALUE1,KEY2=VALUE2 \
  --memory 512Mi \
  --cpu 1
```

#### 管理服務
```bash
# 列出所有服務
gcloud run services list

# 查看服務詳情
gcloud run services describe SERVICE_NAME --region REGION

# 刪除服務
gcloud run services delete SERVICE_NAME --region REGION

# 更新服務環境變數
gcloud run services update SERVICE_NAME \
  --update-env-vars KEY=VALUE \
  --region REGION

# 更新記憶體限制
gcloud run services update SERVICE_NAME \
  --memory 1Gi \
  --region REGION

# 更新 CPU 限制
gcloud run services update SERVICE_NAME \
  --cpu 2 \
  --region REGION

# 更新超時時間
gcloud run services update SERVICE_NAME \
  --timeout 600 \
  --region REGION

# 更新最大實例數
gcloud run services update SERVICE_NAME \
  --max-instances 20 \
  --region REGION

# 移除環境變數
gcloud run services update SERVICE_NAME \
  --remove-env-vars KEY \
  --region REGION
```

#### 查看服務資訊
```bash
# 取得服務 URL
gcloud run services describe SERVICE_NAME \
  --region REGION \
  --format 'value(status.url)'

# 查看服務狀態
gcloud run services describe SERVICE_NAME \
  --region REGION \
  --format 'value(status.conditions)'

# 查看服務配置（YAML）
gcloud run services describe SERVICE_NAME \
  --region REGION \
  --format yaml > service-config.yaml
```

---

### 版本管理（Revisions）

```bash
# 列出所有版本
gcloud run revisions list \
  --service SERVICE_NAME \
  --region REGION

# 查看特定版本
gcloud run revisions describe REVISION_NAME \
  --region REGION

# 查看當前流量分配
gcloud run services describe SERVICE_NAME \
  --region REGION \
  --format 'value(status.traffic)'

# 切換到特定版本
gcloud run services update-traffic SERVICE_NAME \
  --to-revisions REVISION_NAME=100 \
  --region REGION

# 分流到多個版本
gcloud run services update-traffic SERVICE_NAME \
  --to-revisions REVISION_1=70,REVISION_2=30 \
  --region REGION
```

---

### 日誌管理

```bash
# 實時查看日誌（類似 tail -f）
gcloud run services logs tail SERVICE_NAME --region REGION

# 查看最近的日誌
gcloud run services logs read SERVICE_NAME \
  --region REGION \
  --limit 100

# 查看特定時間範圍的日誌
gcloud run services logs read SERVICE_NAME \
  --region REGION \
  --start-time "2025-11-12T10:00:00Z" \
  --end-time "2025-11-12T12:00:00Z"

# 過濾日誌（只顯示錯誤）
gcloud run services logs read SERVICE_NAME \
  --region REGION \
  --log-filter 'severity>=ERROR'

# 搜尋包含特定關鍵字的日誌
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=SERVICE_NAME AND textPayload:關鍵字" \
  --limit=20

# 查看最近 1 小時的日誌
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=SERVICE_NAME" \
  --freshness=1h
```

---

### Container Registry 指令

```bash
# 列出所有映像檔
gcloud container images list

# 列出特定映像的所有版本
gcloud container images list-tags gcr.io/PROJECT_ID/IMAGE_NAME

# 查看映像詳情
gcloud container images describe gcr.io/PROJECT_ID/IMAGE_NAME:latest

# 刪除映像
gcloud container images delete gcr.io/PROJECT_ID/IMAGE_NAME:TAG

# 刪除未標記的映像
gcloud container images list-tags gcr.io/PROJECT_ID/IMAGE_NAME \
  --filter='-tags:*' \
  --format='get(digest)' \
  --limit=unlimited | \
  xargs -I {} gcloud container images delete "gcr.io/PROJECT_ID/IMAGE_NAME@{}" --quiet

# 保留最新 5 個版本，刪除其他
gcloud container images list-tags gcr.io/PROJECT_ID/IMAGE_NAME \
  --sort-by=TIMESTAMP \
  --format='get(digest)' \
  --limit=999999 | \
  tail -n +6 | \
  xargs -I {} gcloud container images delete "gcr.io/PROJECT_ID/IMAGE_NAME@{}" --quiet
```

---

### Artifact Registry 指令

```bash
# 列出所有 repositories
gcloud artifacts repositories list \
  --location=REGION \
  --project=PROJECT_ID

# 創建 repository
gcloud artifacts repositories create REPO_NAME \
  --repository-format=docker \
  --location=REGION \
  --description="Description"

# 刪除 repository
gcloud artifacts repositories delete REPO_NAME \
  --location=REGION \
  --project=PROJECT_ID

# 列出映像
gcloud artifacts docker images list \
  REGION-docker.pkg.dev/PROJECT_ID/REPO_NAME

# 列出特定映像的所有標籤
gcloud artifacts docker images list \
  REGION-docker.pkg.dev/PROJECT_ID/REPO_NAME/IMAGE_NAME \
  --include-tags

# 刪除特定映像版本
gcloud artifacts docker images delete \
  REGION-docker.pkg.dev/PROJECT_ID/REPO_NAME/IMAGE_NAME:TAG
```

---

### Cloud Build 指令

```bash
# 建構並推送映像檔
gcloud builds submit --tag gcr.io/PROJECT_ID/IMAGE_NAME

# 使用 Dockerfile
gcloud builds submit --tag gcr.io/PROJECT_ID/IMAGE_NAME .

# 查看建構歷史
gcloud builds list

# 查看建構日誌
gcloud builds log BUILD_ID

# 取消正在執行的建構
gcloud builds cancel BUILD_ID
```

---

### IAM 和權限管理

```bash
# 創建服務帳號
gcloud iam service-accounts create SERVICE_ACCOUNT_NAME \
  --display-name="Display Name"

# 賦予 Firestore 權限
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:SA_NAME@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/datastore.user"

# 賦予 Secret Manager 權限
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:SA_NAME@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# 查看服務帳號列表
gcloud iam service-accounts list

# 查看服務帳號權限
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:SA_NAME@PROJECT_ID.iam.gserviceaccount.com"
```

---

### Secret Manager 指令

```bash
# 創建 secret
echo -n "secret-value" | gcloud secrets create SECRET_NAME --data-file=-

# 列出所有 secrets
gcloud secrets list

# 查看 secret 版本
gcloud secrets versions list SECRET_NAME

# 讀取 secret 值
gcloud secrets versions access latest --secret=SECRET_NAME

# 刪除 secret
gcloud secrets delete SECRET_NAME

# 檢查 secret 的 IAM 權限
gcloud secrets get-iam-policy SECRET_NAME

# 賦予 secret 訪問權限
gcloud secrets add-iam-policy-binding SECRET_NAME \
  --member="serviceAccount:SA_NAME@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

---

### 啟用 API

```bash
# 啟用 Cloud Run API
gcloud services enable run.googleapis.com

# 啟用 Cloud Build API
gcloud services enable cloudbuild.googleapis.com

# 啟用 Container Registry API
gcloud services enable containerregistry.googleapis.com

# 啟用 Artifact Registry API
gcloud services enable artifactregistry.googleapis.com

# 啟用 Cloud Scheduler API
gcloud services enable cloudscheduler.googleapis.com

# 啟用 Secret Manager API
gcloud services enable secretmanager.googleapis.com

# 列出已啟用的 API
gcloud services list --enabled

# 列出所有可用的 API
gcloud services list --available
```

---

### 過濾與格式化

```bash
# 使用 --format 格式化輸出
gcloud run services list --format="table(name,region,status.url)"

# 只輸出特定欄位
gcloud run services describe SERVICE_NAME --format="value(status.url)"

# JSON 格式輸出
gcloud run services list --format=json

# YAML 格式輸出
gcloud run services list --format=yaml
```

---

### 其他實用指令

```bash
# 查看 gcloud 版本
gcloud version

# 更新 gcloud
gcloud components update

# 列出已安裝的元件
gcloud components list

# 清除快取
gcloud config configurations list

# 切換專案
gcloud config set project NEW_PROJECT_ID

# 查看專案配額
gcloud compute project-info describe --project=PROJECT_ID

# 查看帳單帳戶
gcloud billing accounts list
```

---

## 環境管理

### 多環境部署

#### 開發環境
```bash
gcloud run deploy lunch-selector-dev \
  --image gcr.io/$GCP_PROJECT_ID/lunch-selector:dev \
  --region asia-east1 \
  --memory 256Mi \
  --max-instances 3 \
  --set-env-vars ENV=development
```

#### 測試環境
```bash
gcloud run deploy lunch-selector-staging \
  --image gcr.io/$GCP_PROJECT_ID/lunch-selector:staging \
  --region asia-east1 \
  --memory 512Mi \
  --max-instances 5 \
  --set-env-vars ENV=staging
```

#### 生產環境
```bash
gcloud run deploy lunch-selector \
  --image gcr.io/$GCP_PROJECT_ID/lunch-selector:latest \
  --region asia-east1 \
  --memory 1Gi \
  --cpu 2 \
  --max-instances 10 \
  --min-instances 1 \
  --set-env-vars ENV=production
```

---

### 自定義域名

```bash
# 映射自定義域名
gcloud run domain-mappings create \
  --service lunch-selector \
  --domain lunch.yourdomain.com \
  --region asia-east1

# 查看域名映射
gcloud run domain-mappings list --region asia-east1

# 刪除域名映射
gcloud run domain-mappings delete \
  --domain lunch.yourdomain.com \
  --region asia-east1
```

**設置 DNS 記錄**:
1. 取得 Cloud Run 提供的 DNS 記錄
2. 在域名提供商添加 CNAME 記錄
3. 等待 DNS 傳播（通常 5-10 分鐘）

---

## 故障排除

### 問題 1: 部署失敗 - 權限不足

**症狀**: `Permission denied` 或 `Forbidden`

**解決**:
```bash
# 檢查當前用戶權限
gcloud projects get-iam-policy $GCP_PROJECT_ID

# 賦予必要權限
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="user:your-email@gmail.com" \
  --role="roles/run.admin"

# 或賦予服務帳號權限
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:SA_NAME@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/run.admin"
```

---

### 問題 2: 容器啟動失敗

**症狀**: 服務無法啟動，日誌顯示錯誤

**排查**:
```bash
# 查看最近的錯誤日誌
gcloud run services logs read lunch-selector \
  --region asia-east1 \
  --log-filter 'severity>=ERROR' \
  --limit 50

# 查看特定版本的日誌
gcloud run revisions describe REVISION_NAME \
  --region asia-east1

# 查看服務配置
gcloud run services describe lunch-selector \
  --region asia-east1
```

**常見原因**:
1. 環境變數配置錯誤
2. Firestore 連接問題
3. 記憶體不足（OOM）
4. 啟動超時
5. Secret 訪問權限問題

**解決方案**:
```bash
# 增加記憶體
gcloud run services update lunch-selector \
  --memory 1Gi \
  --region asia-east1

# 增加超時時間
gcloud run services update lunch-selector \
  --timeout 600 \
  --region asia-east1

# 檢查環境變數
gcloud run services describe lunch-selector \
  --region asia-east1 \
  --format="value(spec.template.spec.containers[0].env)"
```

---

### 問題 3: Secret Manager 訪問失敗

**症狀**: 無法讀取 secrets

**解決**:
```bash
# 檢查 secret 是否存在
gcloud secrets list

# 檢查 secret 的 IAM 權限
gcloud secrets get-iam-policy line-channel-token

# 賦予服務帳號權限
gcloud secrets add-iam-policy-binding line-channel-token \
  --member="serviceAccount:lunch-selector-sa@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# 測試讀取 secret
gcloud secrets versions access latest --secret=line-channel-token
```

---

### 問題 4: 記憶體不足（OOM）

**症狀**: 容器被 kill，日誌顯示記憶體不足

**解決**:
```bash
# 增加記憶體限制
gcloud run services update lunch-selector \
  --memory 1Gi \
  --region asia-east1

# 或在 Dockerfile 中調整 JVM 參數
# ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 查看記憶體使用情況（在 GCP Console）
# Cloud Run → 服務 → 指標 → 記憶體使用率
```

---

### 問題 5: 請求超時

**症狀**: 502 Bad Gateway 或 504 Gateway Timeout

**解決**:
```bash
# 增加超時時間（最大 60 分鐘）
gcloud run services update lunch-selector \
  --timeout 600 \
  --region asia-east1

# 檢查應用日誌找出慢查詢
gcloud run services logs read lunch-selector \
  --region asia-east1 | grep "slow"

# 查看請求延遲（在 GCP Console）
# Cloud Run → 服務 → 指標 → 延遲
```

---

### 問題 6: 冷啟動時間過長

**症狀**: 第一次請求需要 5-10 秒

**解決方案**:
```bash
# 1. 設置最小實例數（會增加成本）
gcloud run services update lunch-selector \
  --min-instances 1 \
  --region asia-east1

# 2. 使用 Cloud Scheduler 定期預熱
gcloud scheduler jobs create http lunch-selector-warmup \
  --schedule="*/5 * * * *" \
  --uri="https://YOUR_SERVICE_URL/actuator/health" \
  --http-method=GET \
  --location=asia-east1

# 3. 優化 Docker 映像大小
# 使用多階段構建和 slim 基礎映像
```

---

## 成本優化

### 1. Scale to Zero

```bash
# 設置最小實例數為 0（沒有流量時不收費）⭐ 推薦
gcloud run services update lunch-selector \
  --min-instances 0 \
  --region asia-east1
```

**優點**: 完全按使用量付費
**缺點**: 冷啟動需要 1-3 秒

---

### 2. 資源限制優化

```bash
# 使用較小的記憶體（如果足夠）
gcloud run services update lunch-selector \
  --memory 256Mi \
  --region asia-east1

# 使用較少的 CPU
gcloud run services update lunch-selector \
  --cpu 1 \
  --region asia-east1
```

**提示**: 從小的資源配置開始，根據實際需求逐步增加。

---

### 3. 清理舊版本

```bash
# 刪除超過 30 天的舊版本
gcloud run revisions list \
  --service lunch-selector \
  --region asia-east1 \
  --format='value(metadata.name,metadata.creationTimestamp)' | \
  while read name timestamp; do
    if [[ $(date -d "$timestamp" +%s) -lt $(date -d '30 days ago' +%s) ]]; then
      gcloud run revisions delete $name --region asia-east1 --quiet
    fi
  done

# 或保留最新 5 個版本，刪除其他
gcloud run revisions list \
  --service lunch-selector \
  --region asia-east1 \
  --sort-by="~metadata.creationTimestamp" \
  --format="value(metadata.name)" | \
  tail -n +6 | \
  xargs -I {} gcloud run revisions delete {} --region asia-east1 --quiet
```

---

### 4. 監控成本

```bash
# 查看當前月份的 Cloud Run 費用
gcloud billing accounts list

# 在 GCP Console 查看詳細費用
# https://console.cloud.google.com/billing

# 設置預算告警（在 Console）
# 計費 → 預算與警示 → 建立預算
```

**Cloud Run 定價** (2025):
- CPU: $0.00002400 per vCPU-second
- Memory: $0.00000250 per GiB-second
- Requests: $0.40 per million requests
- **免費額度** (每月):
  - 2 million requests
  - 360,000 vCPU-seconds
  - 180,000 GiB-seconds

---

### 5. 使用免費額度策略

**估算本專案使用量**:
```
假設：
- 每天 100 次請求
- 每次請求平均 1 秒
- 使用 1 vCPU, 512Mi memory

每月：
- 請求數：100 × 30 = 3,000 次 ✅ (遠低於 200 萬)
- CPU 使用：3,000 × 1 = 3,000 秒 ✅ (遠低於 36 萬)
- Memory 使用：3,000 × 0.5 = 1,500 GiB-s ✅ (遠低於 18 萬)

結論：完全在免費額度內！💰 $0/month
```

---

### 6. 映像儲存成本優化

```bash
# 定期清理舊映像（Container Registry）
gcloud container images list-tags gcr.io/$GCP_PROJECT_ID/lunch-selector \
  --sort-by=TIMESTAMP \
  --limit=999999 | \
  tail -n +6 | \
  awk '{print $2}' | \
  xargs -I {} gcloud container images delete "gcr.io/$GCP_PROJECT_ID/lunch-selector@{}" --quiet

# Artifact Registry 清理策略
gcloud artifacts repositories set-cleanup-policy lunch-selector \
  --location=asia-east1 \
  --keep-young-count=5 \
  --dry-run  # 先測試，確認後移除此參數
```

---

## GCP 服務說明

本專案使用的 GCP 服務及其用途：

### 主要服務

#### 1. **Cloud Run** - 運行應用程式容器
- **用途**: 無伺服器容器運行平台
- **優點**:
  - ✅ 自動擴展（scale to zero）
  - ✅ 只按使用量付費
  - ✅ 自動 HTTPS
  - ✅ 全球 CDN
- **定價**: 免費額度內（每月 200 萬次請求）

#### 2. **Artifact Registry** - 儲存 Docker 映像檔
- **用途**: 企業級容器映像儲存
- **優點**:
  - ✅ 細緻的權限控制
  - ✅ 支援多種格式（Docker, Maven, npm）
  - ✅ 自動安全掃描
  - ✅ 未來保障（官方推薦）
- **定價**: 前 0.5GB 免費

#### 3. **Firestore** - NoSQL 文檔資料庫
- **用途**: 儲存用戶餐廳資料
- **優點**:
  - ✅ 即時同步
  - ✅ 自動擴展
  - ✅ 豐富的查詢功能
- **定價**: 免費額度內（每日 50,000 次讀取）

#### 4. **Secret Manager** - 管理敏感資訊
- **用途**: 安全儲存 LINE token 和 secret
- **優點**:
  - ✅ 加密儲存
  - ✅ 版本控制
  - ✅ IAM 權限控制
- **定價**: 前 6 個 secret 免費

#### 5. **Cloud Build** - 建構 Docker 映像檔
- **用途**: 自動化建構流程
- **優點**:
  - ✅ 與 GitHub 整合
  - ✅ 快速建構
  - ✅ 自動部署
- **定價**: 每日 120 分鐘免費

#### 6. **Cloud Logging** - 應用程式日誌
- **用途**: 集中式日誌管理
- **優點**:
  - ✅ 即時查看
  - ✅ 強大的過濾功能
  - ✅ 與 Cloud Monitoring 整合
- **定價**: 每月 50GB 免費

---

### 成本估算

基於目前配置，**完全在 GCP 免費額度內**：

| 服務 | 免費額度 | 預估使用量 | 費用 |
|------|---------|-----------|------|
| Cloud Run | 200 萬次請求/月 | ~3,000 次/月 | $0 |
| Cloud Run CPU | 36 萬 vCPU-秒/月 | ~3,000 秒/月 | $0 |
| Cloud Run Memory | 18 萬 GiB-秒/月 | ~1,500 GiB-秒/月 | $0 |
| Artifact Registry | 0.5GB 儲存 | ~100MB | $0 |
| Firestore | 5 萬次讀取/日 | ~100 次/日 | $0 |
| Firestore | 2 萬次寫入/日 | ~50 次/日 | $0 |
| Secret Manager | 6 個 secret 免費 | 2 個 | $0 |
| Cloud Build | 120 分鐘/天 | ~5 分鐘/天 | $0 |
| Cloud Logging | 50GB/月 | <1GB/月 | $0 |
| **總計** | - | - | **$0** ✨ |

**結論**: 此專案的使用量遠低於 GCP 免費額度，**完全免費**！🎉

---

## 完整部署腳本

創建一個完整的部署腳本 `deploy-to-gcp.sh`:

```bash
#!/bin/bash
set -e

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== 開始部署到 GCP Cloud Run ===${NC}"

# 1. 設置變數
export GCP_PROJECT_ID="mercurial-snow-452117-k6"
export SERVICE_NAME="lunch-selector"
export REGION="asia-east1"
export VERSION=$(date +%Y%m%d-%H%M%S)

echo -e "${BLUE}專案 ID: $GCP_PROJECT_ID${NC}"
echo -e "${BLUE}服務名稱: $SERVICE_NAME${NC}"
echo -e "${BLUE}區域: $REGION${NC}"
echo -e "${BLUE}版本: $VERSION${NC}"

# 2. 驗證環境
echo -e "${YELLOW}[0/6] 驗證環境...${NC}"
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}錯誤: gcloud CLI 未安裝${NC}"
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo -e "${RED}錯誤: Docker 未安裝${NC}"
    exit 1
fi

# 3. 編譯
echo -e "${YELLOW}[1/6] 編譯專案...${NC}"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}編譯失敗${NC}"
    exit 1
fi

# 4. 構建 Docker 映像
echo -e "${YELLOW}[2/6] 構建 Docker 映像...${NC}"
docker build --no-cache -t $SERVICE_NAME:$VERSION .
docker tag $SERVICE_NAME:$VERSION gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME:$VERSION
docker tag $SERVICE_NAME:$VERSION gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME:latest

# 5. 推送到 GCR
echo -e "${YELLOW}[3/6] 推送映像到 GCR...${NC}"
docker push gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME:$VERSION
docker push gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME:latest

# 6. 部署到 Cloud Run
echo -e "${YELLOW}[4/6] 部署到 Cloud Run...${NC}"
gcloud run deploy $SERVICE_NAME \
  --image gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME:latest \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --timeout 300 \
  --max-instances 10 \
  --min-instances 0 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID \
  --quiet

if [ $? -ne 0 ]; then
    echo -e "${RED}部署失敗${NC}"
    exit 1
fi

# 7. 驗證部署
echo -e "${YELLOW}[5/6] 驗證部署...${NC}"
export SERVICE_URL=$(gcloud run services describe $SERVICE_NAME \
  --region $REGION \
  --format 'value(status.url)')

echo -e "${GREEN}服務 URL: $SERVICE_URL${NC}"

# 8. 測試 API
echo -e "${YELLOW}[6/6] 測試 API...${NC}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $SERVICE_URL/api/users/TEST_USER_001/restaurants)

if [ $HTTP_CODE -eq 200 ]; then
    echo -e "${GREEN}✅ API 測試通過 (HTTP $HTTP_CODE)${NC}"
else
    echo -e "${RED}❌ API 測試失敗 (HTTP $HTTP_CODE)${NC}"
fi

# 9. 顯示摘要
echo -e "${GREEN}=== 部署完成！ ===${NC}"
echo -e "${BLUE}版本: $VERSION${NC}"
echo -e "${BLUE}映像: gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME:$VERSION${NC}"
echo -e "${BLUE}服務 URL: $SERVICE_URL${NC}"

# 10. 顯示後續步驟
echo -e "\n${YELLOW}後續步驟：${NC}"
echo "1. 查看日誌: gcloud run services logs tail $SERVICE_NAME --region $REGION"
echo "2. 查看服務: gcloud run services describe $SERVICE_NAME --region $REGION"
echo "3. 查看指標: https://console.cloud.google.com/run/detail/$REGION/$SERVICE_NAME"
```

**使用方法**:
```bash
# 賦予執行權限
chmod +x deploy-to-gcp.sh

# 執行部署
./deploy-to-gcp.sh
```

---

## 監控和告警

### 設置 Cloud Monitoring

```bash
# 創建告警策略（當錯誤率超過 5% 時）
gcloud alpha monitoring policies create \
  --notification-channels=CHANNEL_ID \
  --display-name="Lunch Selector High Error Rate" \
  --condition-display-name="Error rate > 5%" \
  --condition-threshold-value=5.0 \
  --condition-threshold-duration=300s

# 創建 Uptime Check（每 5 分鐘檢查一次）
gcloud monitoring uptime create lunch-selector-check \
  --resource-type=uptime-url \
  --resource-labels=host=YOUR_SERVICE_URL \
  --http-check-path=/actuator/health \
  --display-name="Lunch Selector Uptime Check" \
  --period=300
```

---

### 查看指標

在 GCP Console:
1. 前往 **Cloud Run** → 選擇服務
2. 查看 **指標** 頁籤
3. 可查看：
   - 請求數
   - 延遲（P50, P95, P99）
   - 錯誤率
   - 容器實例數
   - 記憶體/CPU 使用率
   - 冷啟動時間

**常用指標查詢**:
```
# 請求數
resource.type="cloud_run_revision"
metric.type="run.googleapis.com/request_count"

# 錯誤率
resource.type="cloud_run_revision"
metric.type="run.googleapis.com/request_count"
metric.label.response_code_class="5xx"

# 延遲
resource.type="cloud_run_revision"
metric.type="run.googleapis.com/request_latencies"
```

---

## 參考資源

### 官方文檔
- [Cloud Run 官方文檔](https://cloud.google.com/run/docs)
- [Cloud Run 定價](https://cloud.google.com/run/pricing)
- [gcloud run 命令參考](https://cloud.google.com/sdk/gcloud/reference/run)
- [Artifact Registry 文檔](https://cloud.google.com/artifact-registry/docs)
- [Secret Manager 文檔](https://cloud.google.com/secret-manager/docs)
- [Firestore 文檔](https://cloud.google.com/firestore/docs)

### 專案相關
- [專案 GCP Console](https://console.cloud.google.com/run?project=mercurial-snow-452117-k6)
- [專案 Dockerfile](./Dockerfile)
- [Docker 部署指南](./DOCKER_DEPLOYMENT.md)

### 學習資源
- [Cloud Run 快速入門](https://cloud.google.com/run/docs/quickstarts)
- [Cloud Run 最佳實踐](https://cloud.google.com/run/docs/best-practices)
- [GCP 免費方案](https://cloud.google.com/free)

---

## 總結

### 核心原則

1. ✅ **使用 Artifact Registry** - 官方推薦，功能更強
2. ✅ **使用 Secret Manager** - 安全管理敏感資訊
3. ✅ **Scale to Zero** - 按使用量付費，降低成本
4. ✅ **版本標籤** - 便於追蹤和回滾
5. ✅ **最小權限** - IAM 權限控制
6. ✅ **監控告警** - 及時發現問題
7. ✅ **定期清理** - 刪除舊版本和映像
8. ✅ **利用免費額度** - 充分利用 GCP 免費方案

### 推薦工作流程

**首次部署**:
```bash
# 1. 設置環境
gcloud init
gcloud services enable run.googleapis.com artifactregistry.googleapis.com

# 2. 創建 repository
gcloud artifacts repositories create lunch-selector \
  --repository-format=docker --location=asia-east1

# 3. 執行部署腳本
./deploy-to-gcp.sh
```

**日常更新**:
```bash
# 快速更新（使用部署腳本）
./deploy-to-gcp.sh

# 或手動更新
mvn clean package -DskipTests
docker build --no-cache -t lunch-selector:$(date +%Y%m%d-%H%M%S) .
docker tag lunch-selector:$(date +%Y%m%d-%H%M%S) gcr.io/$GCP_PROJECT_ID/lunch-selector:latest
docker push gcr.io/$GCP_PROJECT_ID/lunch-selector:latest
gcloud run deploy lunch-selector --image gcr.io/$GCP_PROJECT_ID/lunch-selector:latest --region asia-east1
```

**定期維護**:
```bash
# 每月清理舊版本
gcloud run revisions list --service lunch-selector --region asia-east1 --sort-by="~metadata.creationTimestamp" | tail -n +6
# 每月清理舊映像
gcloud container images list-tags gcr.io/$GCP_PROJECT_ID/lunch-selector --sort-by=TIMESTAMP | tail -n +6
```

---

**更新日期**: 2025-11-12
**版本**: 2.0.0
**作者**: Lunch Selector Team
