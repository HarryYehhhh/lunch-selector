# 🐳 Docker 部署完整指南

## 📋 目錄
- [快速開始](#快速開始)
- [前置準備](#前置準備)
- [本地 Docker 部署](#本地-docker-部署)
- [Docker 常用指令](#docker-常用指令)
- [完整部署流程](#完整部署流程)
- [故障排除](#故障排除)
- [Docker 最佳實踐](#docker-最佳實踐)
- [緩存管理策略](#緩存管理策略)
- [快速參考卡](#快速參考卡)
- [監控和維護](#監控和維護)

---

## 快速開始

### 日常開發流程（最常用）

#### 1. 快速構建和運行（使用緩存）
```bash
# 編譯
mvn clean package -DskipTests

# 構建（使用緩存，速度快）
docker build -t lunch-selector:latest .

# 停止並刪除舊容器
docker stop lunch-selector 2>/dev/null
docker rm lunch-selector 2>/dev/null

# 運行新容器
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest

# 查看日誌
docker logs -f lunch-selector
```

#### 2. 確保構建正確（不使用緩存）⭐ 推薦
```bash
# 完全重新構建
mvn clean package -DskipTests && \
docker build --no-cache -t lunch-selector:latest . && \
docker stop lunch-selector 2>/dev/null; docker rm lunch-selector 2>/dev/null; \
docker run -d --name lunch-selector -p 8080:8080 --env-file .env.local \
-v $(pwd)/firestore-key.json:/app/firestore-key.json lunch-selector:latest
```

#### 3. 查看運行狀態
```bash
# 查看容器狀態
docker ps | grep lunch-selector

# 查看日誌（最近 30 行）
docker logs --tail 30 lunch-selector

# 實時跟蹤日誌（Ctrl+C 退出）
docker logs -f lunch-selector
```

#### 4. 重啟容器
```bash
docker restart lunch-selector
```

#### 5. 清理資源
```bash
# 清理未使用的 image 和 container
docker system prune -f

# 清理所有緩存（慎用）
docker system prune -a --volumes
```

---

## 前置準備

### 1. 安裝 Docker

**macOS**:
```bash
# 使用 Homebrew 安裝
brew install --cask docker

# 或從官網下載
# https://www.docker.com/products/docker-desktop
```

**驗證安裝**:
```bash
docker --version
docker-compose --version
```

**預期輸出**:
```
Docker version 24.0.6, build ed223bc
Docker Compose version v2.23.0
```

### 2. 準備環境變數文件

確保 `.env.local` 存在且包含必要變數：
```bash
cat .env.local
```

應該包含：
```bash
export GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/firestore-key.json"
export GCP_PROJECT_ID="your-project-id"
export LINE_CHANNEL_TOKEN="your-token"
export LINE_CHANNEL_SECRET="your-secret"
```

### 3. 確認 Firestore 金鑰文件存在
```bash
ls -la firestore-key.json
```

**預期輸出**:
```
-rw-r--r--  1 user  staff  2345  Nov 12 10:00 firestore-key.json
```

---

## 本地 Docker 部署

### 步驟 1: 編譯專案

```bash
# 清理並編譯（跳過測試）
mvn clean package -DskipTests

# 驗證 JAR 檔案是否生成
ls -lh target/lunch-selector-1.0.0.jar
```

**預期輸出**:
```
-rw-r--r--  1 user  staff   50M  Nov 12 10:00 target/lunch-selector-1.0.0.jar
```

---

### 步驟 2: 構建 Docker 映像

#### 選項 A: 使用快取（開發環境，速度快）
```bash
docker build -t lunch-selector:latest .
```

**優點**: 構建速度快（2-3 分鐘）
**缺點**: 可能使用舊的緩存層

#### 選項 B: 不使用快取（生產環境，確保最新）⭐ 推薦
```bash
docker build --no-cache -t lunch-selector:latest .
```

**優點**: 確保所有程式碼都是最新的
**缺點**: 構建時間較長（5-10 分鐘）

#### 選項 C: 使用特定標籤
```bash
# 使用版本號
docker build --no-cache -t lunch-selector:1.0.0 .

# 使用時間戳
docker build --no-cache -t lunch-selector:$(date +%Y%m%d-%H%M%S) .

# 使用 Git commit hash
docker build --no-cache -t lunch-selector:$(git rev-parse --short HEAD) .
```

**構建時間**:
- 使用緩存：約 2-3 分鐘
- 不使用緩存：約 5-10 分鐘

**驗證映像**:
```bash
docker images | grep lunch-selector
```

**預期輸出**:
```
lunch-selector   latest    2c9790403c8d   2 minutes ago   323MB
```

---

### 步驟 3: 運行容器

#### 基本運行
```bash
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest
```

**參數說明**:
- `-d`: 背景運行（daemon mode）
- `--name lunch-selector`: 容器名稱
- `-p 8080:8080`: 端口映射（主機:容器）
- `--env-file .env.local`: 載入環境變數
- `-v $(pwd)/firestore-key.json:/app/firestore-key.json`: 掛載金鑰文件

#### 進階運行選項

**指定不同端口**:
```bash
# 使用 8081 端口
docker run -d \
  --name lunch-selector \
  -p 8081:8080 \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest
```

**設置重啟策略**:
```bash
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --restart unless-stopped \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest
```

**重啟策略選項**:
- `no`: 不自動重啟（預設）
- `on-failure`: 只有在容器非正常退出時重啟
- `always`: 總是重啟
- `unless-stopped`: 除非手動停止，否則總是重啟 ⭐ 推薦

**設置資源限制**:
```bash
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --memory="512m" \
  --cpus="1.0" \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest
```

**設置日誌輪轉**:
```bash
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest
```

---

### 步驟 4: 驗證部署

```bash
# 1. 檢查容器狀態
docker ps | grep lunch-selector

# 2. 查看日誌
docker logs lunch-selector

# 3. 測試 API
curl http://localhost:8080/api/users/TEST_USER_001/restaurants

# 4. 進入容器（如需調試）
docker exec -it lunch-selector /bin/sh
```

**預期結果**:
- 容器狀態應該是 "Up"
- 日誌中應該看到 "Tomcat started on port 8080"
- API 應該返回 JSON 資料

---

## Docker 常用指令

### 容器管理

#### 啟動和停止
```bash
# 停止容器
docker stop lunch-selector

# 啟動已停止的容器
docker start lunch-selector

# 重啟容器
docker restart lunch-selector

# 強制停止容器
docker kill lunch-selector

# 刪除容器（必須先停止）
docker rm lunch-selector

# 強制刪除運行中的容器
docker rm -f lunch-selector
```

#### 查看信息
```bash
# 查看所有運行中的容器
docker ps

# 查看所有容器（包括已停止的）
docker ps -a

# 查看容器詳細信息
docker inspect lunch-selector

# 查看容器資源使用情況
docker stats lunch-selector

# 查看容器端口映射
docker port lunch-selector

# 查看容器進程
docker top lunch-selector
```

#### 日誌管理
```bash
# 查看所有日誌
docker logs lunch-selector

# 查看最近 100 行日誌
docker logs --tail 100 lunch-selector

# 實時跟蹤日誌（Ctrl+C 退出）
docker logs -f lunch-selector

# 查看帶時間戳的日誌
docker logs -t lunch-selector

# 查看特定時間後的日誌
docker logs --since 2025-11-12T10:00:00 lunch-selector

# 查看最近 10 分鐘的日誌
docker logs --since 10m lunch-selector
```

#### 容器操作
```bash
# 進入運行中的容器
docker exec -it lunch-selector /bin/sh

# 在容器中執行命令
docker exec lunch-selector ls -la /app

# 複製文件到容器
docker cp local-file.txt lunch-selector:/app/

# 從容器複製文件
docker cp lunch-selector:/app/logs/app.log ./

# 查看容器的環境變數
docker exec lunch-selector env
```

---

### 映像管理

#### 查看和搜尋
```bash
# 查看所有映像
docker images

# 查看特定映像
docker images lunch-selector

# 搜尋映像
docker search openjdk

# 查看映像歷史
docker history lunch-selector:latest

# 查看映像詳細信息
docker inspect lunch-selector:latest
```

#### 刪除映像
```bash
# 刪除映像
docker rmi lunch-selector:latest

# 刪除特定版本
docker rmi lunch-selector:1.0.0

# 強制刪除（即使有容器在使用）
docker rmi -f lunch-selector:latest

# 刪除所有懸空映像（<none>）
docker image prune

# 刪除所有未使用的映像
docker image prune -a

# 刪除所有映像（危險操作）
docker rmi $(docker images -q)
```

#### 標籤管理
```bash
# 為映像添加新標籤
docker tag lunch-selector:latest lunch-selector:stable

# 為映像添加版本標籤
docker tag lunch-selector:latest lunch-selector:1.0.0

# 為推送到 GCR 創建標籤
docker tag lunch-selector:latest gcr.io/YOUR_PROJECT_ID/lunch-selector:latest

# 為推送到 Artifact Registry 創建標籤
docker tag lunch-selector:latest asia-east1-docker.pkg.dev/YOUR_PROJECT_ID/lunch-selector/app:latest
```

#### 導入和導出
```bash
# 導出映像為 tar 檔案
docker save lunch-selector:latest > lunch-selector-backup.tar

# 或使用 gzip 壓縮
docker save lunch-selector:latest | gzip > lunch-selector-backup.tar.gz

# 導入映像
docker load < lunch-selector-backup.tar

# 或從壓縮檔導入
docker load < lunch-selector-backup.tar.gz
```

---

### 清理命令

```bash
# 刪除所有已停止的容器
docker container prune

# 刪除所有未使用的映像
docker image prune -a

# 刪除所有未使用的網絡
docker network prune

# 刪除所有未使用的卷
docker volume prune

# 一次清理所有（容器、映像、網絡、卷）
docker system prune -a --volumes

# 清理但保留最近 24 小時的資源
docker system prune -a --filter "until=24h"

# 查看 Docker 磁盤使用情況
docker system df

# 查看詳細的磁盤使用情況
docker system df -v
```

---

## 完整部署流程

### 開發環境部署（快速迭代）

```bash
# 1. 編譯
mvn clean package -DskipTests

# 2. 構建（使用快取）
docker build -t lunch-selector:latest .

# 3. 停止並刪除舊容器
docker stop lunch-selector 2>/dev/null
docker rm lunch-selector 2>/dev/null

# 4. 運行新容器
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest

# 5. 查看日誌
docker logs -f lunch-selector
```

---

### 生產環境部署（完全重建）

```bash
# 1. 編譯（包含測試）
mvn clean package

# 2. 構建（不使用快取）
VERSION=$(date +%Y%m%d-%H%M%S)
docker build --no-cache -t lunch-selector:$VERSION .
docker tag lunch-selector:$VERSION lunch-selector:latest

# 3. 停止舊容器
docker stop lunch-selector 2>/dev/null
docker rm lunch-selector 2>/dev/null

# 4. 運行新容器（帶重啟策略和資源限制）
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --restart unless-stopped \
  --memory="512m" \
  --cpus="1.0" \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest

# 5. 驗證部署
docker ps | grep lunch-selector
docker logs --tail 50 lunch-selector
curl http://localhost:8080/api/users/TEST_USER_001/restaurants

# 6. 清理舊映像
docker image prune -a -f
```

---

## 故障排除

### 問題 1: 容器無法啟動

**症狀**: `docker ps` 沒有看到容器

**排查**:
```bash
# 查看所有容器（包括已停止的）
docker ps -a

# 查看日誌
docker logs lunch-selector

# 查看退出代碼
docker inspect lunch-selector | grep ExitCode

# 查看完整錯誤信息
docker inspect lunch-selector --format='{{.State}}'
```

**常見原因**:
1. 端口已被占用
2. 環境變數配置錯誤
3. Firestore 金鑰文件路徑錯誤
4. 記憶體不足

---

### 問題 2: 端口衝突

**症狀**: `Error: port is already allocated`

**解決**:
```bash
# 查看哪個進程占用了端口
lsof -i :8080

# 停止占用端口的進程
kill -9 <PID>

# 或使用不同端口
docker run -d --name lunch-selector -p 8081:8080 ...

# 或停止占用端口的容器
docker ps | grep 8080
docker stop <container_id>
```

---

### 問題 3: Firestore 連接失敗

**症狀**: 日誌中出現 "Firestore 初始化失敗"

**排查**:
```bash
# 1. 檢查容器內的金鑰文件
docker exec lunch-selector ls -la /app/firestore-key.json

# 2. 檢查環境變數
docker exec lunch-selector env | grep GCP

# 3. 驗證金鑰文件內容
docker exec lunch-selector cat /app/firestore-key.json | head -5

# 4. 測試 Firestore 連接
docker exec lunch-selector curl -H "Authorization: Bearer $(gcloud auth print-access-token)" \
  https://firestore.googleapis.com/v1/projects/YOUR_PROJECT_ID/databases
```

**解決**:
- 確認 firestore-key.json 存在且路徑正確
- 確認 GCP_PROJECT_ID 設置正確
- 確認金鑰文件有效且未過期
- 確認金鑰文件有 Firestore 權限

---

### 問題 4: 容器占用過多資源

**症狀**: 系統變慢，風扇全速運轉

**排查**:
```bash
# 查看資源使用情況
docker stats lunch-selector

# 查看所有容器的資源使用
docker stats

# 查看容器進程
docker top lunch-selector
```

**解決 - 限制資源**:
```bash
# 停止容器
docker stop lunch-selector
docker rm lunch-selector

# 重新運行並限制資源
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --memory="512m" \
  --memory-swap="512m" \
  --cpus="1.0" \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest
```

---

### 問題 5: 映像構建失敗

**症狀**: Docker build 過程中出錯

**排查**:
```bash
# 查看詳細構建日誌
docker build --no-cache --progress=plain -t lunch-selector:latest .

# 檢查 Dockerfile 語法
cat Dockerfile

# 檢查 .dockerignore
cat .dockerignore

# 檢查是否有足夠的磁盤空間
df -h

# 清理 Docker 緩存
docker system prune -a
```

**常見原因**:
1. 磁盤空間不足
2. 網絡問題（下載依賴失敗）
3. Dockerfile 語法錯誤
4. Maven 編譯失敗

---

### 問題 6: 構建包含舊代碼

**症狀**: 新功能沒有生效，容器運行的是舊版本

**原因**: Docker 緩存機制導致

**解決**:
```bash
# 方案 1：完全不使用緩存
docker build --no-cache -t lunch-selector:latest .

# 方案 2：清理緩存後重建
docker system prune -a
docker build -t lunch-selector:latest .

# 方案 3：使用新標籤強制重建
docker build -t lunch-selector:$(date +%Y%m%d-%H%M%S) .

# 驗證構建結果
docker run --rm lunch-selector:latest jar -tf /app/app.jar | grep "YourNewClass"
```

---

## Docker 最佳實踐

### 1. 使用多階段構建

當前 Dockerfile 已經使用多階段構建：
```dockerfile
# 階段 1: 構建
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 階段 2: 運行
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/lunch-selector-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

**優點**:
- ✅ 最小化最終映像大小（約 300MB vs 800MB）
- ✅ 不包含構建工具（Maven、源碼）
- ✅ 更安全（減少攻擊面）
- ✅ 更快的部署速度

---

### 2. 使用 .dockerignore

確保 `.dockerignore` 排除不必要的文件：
```
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties

# IDE
.idea/
*.iml
.vscode/
.DS_Store

# Git
.git/
.gitignore

# Logs
*.log
logs/

# Environment
.env
.env.local
*.key
*.json

# Build artifacts
*.class
*.jar
*.war

# Node
node_modules/
npm-debug.log

# Temporary
*.tmp
*.temp
*.swp
```

**為什麼重要**:
- IDE 配置改變 → 不應該觸發重新構建
- Git commit → 不應該影響緩存
- 日誌文件 → 不應該被複製
- 減少 context 大小，加快構建速度

---

### 3. 版本標籤管理

```bash
# ❌ 不好的做法
docker build -t lunch-selector:latest .

# ✅ 好的做法 - 使用多個標籤
VERSION=$(date +%Y%m%d-%H%M%S)
docker build -t lunch-selector:$VERSION \
             -t lunch-selector:latest \
             -t lunch-selector:1.0.0 .

# ✅ 使用 Git commit hash
docker build -t lunch-selector:$(git rev-parse --short HEAD) .

# ✅ 使用語義化版本
docker build -t lunch-selector:1.0.0 .
docker build -t lunch-selector:1.0 .
docker build -t lunch-selector:1 .
docker build -t lunch-selector:latest .
```

**好處**:
- 便於追蹤和回滾
- 明確知道運行的是哪個版本
- 可以同時保留多個版本

---

### 4. 健康檢查

在 Dockerfile 中添加健康檢查：
```dockerfile
# 添加 curl（如果基礎映像沒有）
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

或在運行時添加：
```bash
docker run -d \
  --name lunch-selector \
  --health-cmd="curl -f http://localhost:8080/actuator/health || exit 1" \
  --health-interval=30s \
  --health-timeout=3s \
  --health-start-period=40s \
  --health-retries=3 \
  ...
```

**查看健康狀態**:
```bash
docker ps  # 查看 STATUS 欄位
docker inspect lunch-selector | grep -A 10 Health
```

---

### 5. 日誌管理

防止日誌文件過大：
```bash
docker run -d \
  --name lunch-selector \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  ...
```

**配置說明**:
- `max-size=10m`: 單個日誌文件最大 10MB
- `max-file=3`: 最多保留 3 個日誌文件
- 總日誌大小：10MB × 3 = 30MB

---

### 6. 安全實踐

```bash
# 1. 不要在映像中存儲敏感信息
# ❌ 不要這樣做
ENV LINE_CHANNEL_TOKEN="your-secret-token"

# ✅ 使用環境變數或掛載卷
docker run -e LINE_CHANNEL_TOKEN=$TOKEN ...
docker run --env-file .env.local ...

# 2. 定期更新基礎映像
docker pull openjdk:17-jdk-slim
docker build --no-cache -t lunch-selector:latest .

# 3. 掃描安全漏洞（Docker Desktop 內建）
docker scan lunch-selector:latest

# 4. 使用非 root 用戶運行（在 Dockerfile 中）
RUN useradd -m -u 1000 appuser
USER appuser

# 5. 最小化映像（使用 slim 或 alpine 版本）
FROM openjdk:17-jdk-slim  # ✅ 好
FROM openjdk:17-jdk-alpine  # ✅ 更好（更小）
FROM openjdk:17-jdk  # ❌ 避免使用（太大）
```

---

### 7. 優化 Dockerfile 結構

#### ❌ 不好的做法
```dockerfile
FROM maven:3.9-openjdk-17
WORKDIR /app
COPY . .                    # 複製所有東西
RUN mvn clean package       # IDE 配置改變也會重新構建
```

#### ✅ 好的做法
```dockerfile
FROM maven:3.9-openjdk-17 AS build
WORKDIR /app

# Step 1: 先複製依賴文件（變動少）
COPY pom.xml .
RUN mvn dependency:go-offline

# Step 2: 再複製源碼（變動多）
COPY src ./src
RUN mvn clean package -DskipTests -B

# Step 3: 驗證構建結果
RUN jar -tf target/app.jar | grep "critical/Class.class" || exit 1

# Stage 2: 運行
FROM openjdk:17-jdk-slim
COPY --from=build /app/target/app.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]
```

**優點**:
- 依賴層緩存穩定（pom.xml 很少變）
- 源碼變更不影響依賴下載
- 構建速度快

---

## 緩存管理策略

### Docker 緩存機制

Docker 的緩存基於**內容的 hash 值**，而非文件修改時間：

```dockerfile
COPY src ./src              # ← 如果內容 hash 沒變，使用緩存
RUN mvn clean package       # ← 前一層用緩存，這層也用緩存
```

**問題**: 即使新增了文件，舊的緩存層可能不包含這個文件。

---

### 不同環境的緩存策略

| 環境 | 策略 | 命令 | 原因 |
|------|------|------|------|
| 本地開發 | 使用緩存 | `docker build -t app .` | 提高速度，快速迭代 |
| 提交前測試 | 不使用緩存 | `docker build --no-cache -t app .` | 確保沒問題 |
| CI/CD | 完全不使用緩存 | `docker build --no-cache -t app .` | 確保可靠性和可重現性 |
| 定期維護 | 清理緩存 | `docker builder prune -af` | 避免累積問題 |

---

### 實務構建策略

#### 場景 1：本地開發（日常使用）
```bash
# 快速迭代，使用緩存
docker build -t lunch-selector:latest .
docker stop lunch-selector 2>/dev/null && docker rm lunch-selector 2>/dev/null
docker run -d --name lunch-selector -p 8080:8080 --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json lunch-selector:latest
```

#### 場景 2：提交代碼前測試
```bash
# 完全重新構建，確保沒問題
docker build --no-cache -t lunch-selector:latest .
docker run -d --name lunch-selector -p 8080:8080 --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json lunch-selector:latest
# 測試所有功能...
```

#### 場景 3：CI/CD 自動部署
```yaml
# GitHub Actions / GitLab CI
- name: Build Docker image
  run: docker build --no-cache -t myapp:${{ github.sha }} .
```

#### 場景 4：定期清理（每週一次）
```bash
# 清理所有 Docker 緩存
docker builder prune -af
docker system prune -a --volumes

# 重新構建
docker build -t lunch-selector:latest .
```

---

### 緩存失效檢查清單

當你懷疑緩存有問題時：

**1. 檢查 .dockerignore**
```bash
cat .dockerignore
```

**2. 查看構建日誌中的緩存使用**
```bash
docker build . 2>&1 | grep "CACHED"
```

**3. 清理所有緩存**
```bash
docker builder prune -af
docker build --no-cache .
```

**4. 驗證 JAR 內容**
```bash
docker run --rm lunch-selector:latest jar -tf /app/app.jar | grep "YourNewClass"
```

**5. 比較映像大小**
```bash
docker images lunch-selector
# 如果大小和之前一樣，可能用了緩存
```

---

### 緩存相關常見錯誤

#### ❌ 錯誤 1：依賴 `latest` 標籤
```bash
docker build -t myapp:latest .
docker run myapp:latest
```

**問題**: 無法追蹤版本，rollback 困難

#### ✅ 修正：使用明確版本
```bash
VERSION=$(date +%Y%m%d-%H%M%S)
docker build -t myapp:$VERSION -t myapp:latest .
```

---

#### ❌ 錯誤 2：在 Dockerfile 中使用動態內容
```dockerfile
RUN echo "$(date)" > /tmp/build-time.txt  # ← 每次都不同，破壞緩存
```

#### ✅ 修正：使用構建參數
```dockerfile
ARG BUILD_DATE
LABEL build_date="${BUILD_DATE}"
```

```bash
docker build --build-arg BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ") .
```

---

#### ❌ 錯誤 3：複製不必要的文件
```dockerfile
COPY . .  # ← 包含 .git, target, node_modules 等
```

#### ✅ 修正：使用 .dockerignore
```dockerignore
.git
target/
node_modules/
*.log
.DS_Store
```

---

## 快速參考卡

### 常用命令組合

```bash
# 重新部署（開發環境）
mvn clean package -DskipTests && \
docker build -t lunch-selector:latest . && \
docker stop lunch-selector 2>/dev/null; docker rm lunch-selector 2>/dev/null; \
docker run -d --name lunch-selector -p 8080:8080 --env-file .env.local \
-v $(pwd)/firestore-key.json:/app/firestore-key.json lunch-selector:latest

# 重新部署（生產環境，不使用緩存）
mvn clean package && \
docker build --no-cache -t lunch-selector:$(date +%Y%m%d-%H%M%S) . && \
docker tag lunch-selector:$(date +%Y%m%d-%H%M%S) lunch-selector:latest && \
docker stop lunch-selector 2>/dev/null; docker rm lunch-selector 2>/dev/null; \
docker run -d --name lunch-selector -p 8080:8080 --restart unless-stopped \
--env-file .env.local -v $(pwd)/firestore-key.json:/app/firestore-key.json \
lunch-selector:latest

# 查看日誌和狀態
docker ps | grep lunch-selector && docker logs --tail 50 lunch-selector

# 實時查看日誌
docker logs -f --tail 100 lunch-selector

# 完全清理重來
docker stop lunch-selector && docker rm lunch-selector && \
docker rmi lunch-selector:latest && \
docker system prune -f

# 進入容器調試
docker exec -it lunch-selector /bin/sh

# 查看容器內的文件
docker exec lunch-selector ls -la /app
docker exec lunch-selector cat /app/app.log

# 複製文件到容器
docker cp local-file.txt lunch-selector:/app/

# 從容器複製文件
docker cp lunch-selector:/app/logs/app.log ./

# 查看容器資源使用
docker stats lunch-selector --no-stream

# 檢查容器健康狀態
docker inspect lunch-selector | grep -A 10 Health
```

---

## 監控和維護

### 日常檢查腳本

創建 `docker-check.sh`:
```bash
#!/bin/bash

echo "=== 容器狀態 ==="
docker ps | grep lunch-selector

echo -e "\n=== 資源使用 ==="
docker stats --no-stream lunch-selector

echo -e "\n=== 最近日誌 ==="
docker logs --tail 20 lunch-selector

echo -e "\n=== 磁盤使用 ==="
docker system df

echo -e "\n=== 映像信息 ==="
docker images | grep lunch-selector

echo -e "\n=== 健康檢查 ==="
docker inspect lunch-selector | grep -A 5 Health
```

使用方法:
```bash
chmod +x docker-check.sh
./docker-check.sh
```

---

### 自動化清理腳本

創建 `docker-cleanup.sh`:
```bash
#!/bin/bash

echo "清理已停止的容器..."
docker container prune -f

echo "清理未使用的映像..."
docker image prune -a -f

echo "清理未使用的網絡..."
docker network prune -f

echo "清理未使用的卷..."
docker volume prune -f

echo "清理 build 緩存..."
docker builder prune -f

echo "磁盤使用情況："
docker system df
```

---

### 備份和恢復

#### 備份容器數據
```bash
# 備份映像
docker save lunch-selector:latest | gzip > lunch-selector-backup-$(date +%Y%m%d).tar.gz

# 備份容器卷（如果有）
docker run --rm \
  -v lunch-selector-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/volume-backup-$(date +%Y%m%d).tar.gz /data

# 備份容器配置
docker inspect lunch-selector > lunch-selector-config-$(date +%Y%m%d).json
```

#### 恢復
```bash
# 恢復映像
docker load < lunch-selector-backup.tar.gz

# 恢復卷
docker run --rm \
  -v lunch-selector-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/volume-backup.tar.gz -C /
```

---

### 性能監控

```bash
# 實時監控容器資源使用
docker stats lunch-selector

# 查看容器詳細統計
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"

# 導出統計數據到文件
while true; do
  docker stats --no-stream lunch-selector >> docker-stats-$(date +%Y%m%d).log
  sleep 60
done
```

---

## 參考資源

### 官方文檔
- [Docker 官方文檔](https://docs.docker.com/)
- [Dockerfile 最佳實踐](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Docker Hub](https://hub.docker.com/)
- [多階段構建](https://docs.docker.com/build/building/multi-stage/)

### 專案相關
- [專案 Dockerfile](./Dockerfile)
- [專案 .dockerignore](./.dockerignore)

### 學習資源
- [Docker 快速入門](https://docs.docker.com/get-started/)
- [Docker Compose 教學](https://docs.docker.com/compose/gettingstarted/)

---

## 總結

### 核心原則

1. ✅ **開發時用緩存** - 提高速度
2. ✅ **部署時不用緩存** - 確保可靠
3. ✅ **使用版本標籤** - 便於追蹤和回滾
4. ✅ **定期清理緩存** - 避免累積問題
5. ✅ **使用 .dockerignore** - 減少 context 大小
6. ✅ **多階段構建** - 最小化映像大小
7. ✅ **健康檢查** - 確保服務正常運行
8. ✅ **日誌輪轉** - 防止日誌文件過大
9. ✅ **資源限制** - 防止容器占用過多資源
10. ✅ **安全實踐** - 定期更新、掃描漏洞

### 推薦工作流程

**日常開發** (使用緩存):
```bash
mvn clean package -DskipTests
docker build -t lunch-selector:latest .
docker stop lunch-selector && docker rm lunch-selector
docker run -d --name lunch-selector -p 8080:8080 --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json lunch-selector:latest
```

**提交前驗證** (不使用緩存):
```bash
docker build --no-cache -t lunch-selector:latest .
# 完整測試...
git add . && git commit -m "..." && git push
```

**每週維護**:
```bash
docker system prune -a
docker builder prune -af
```

---

**更新日期**: 2025-11-12
**版本**: 2.0.0
**作者**: Lunch Selector Team
