# 🧪 手動測試 API 完整指南

## 📋 目錄
- [準備工作](#準備工作)
- [推薦測試工具](#推薦測試工具)
- [啟動應用](#啟動應用)
- [測試步驟](#測試步驟)
- [測試範例](#測試範例)

---

## 準備工作

### 1. 確認環境
```bash
# 1. 檢查 Java 版本
java -version  # 需要 Java 17+

# 2. 檢查環境變數
source .env.local
echo $GCP_PROJECT_ID
echo $LINE_CHANNEL_TOKEN

# 3. 確認 Firestore 金鑰存在
ls -la firestore-key.json
```

---

## 推薦測試工具

### 🥇 選項 1: Postman (最推薦，功能最強大)
- **優點**: 圖形化界面、可保存請求、自動補全、環境變數
- **下載**: https://www.postman.com/downloads/
- **適合**: 所有用戶，特別是初學者

### 🥈 選項 2: Insomnia (輕量級)
- **優點**: 簡潔界面、快速、支援 GraphQL
- **下載**: https://insomnia.rest/download
- **適合**: 喜歡簡單界面的用戶

### 🥉 選項 3: VS Code Extension - Thunder Client
- **優點**: 直接在 VS Code 中使用、無需切換視窗
- **安裝**: VS Code Extensions → 搜尋 "Thunder Client"
- **適合**: VS Code 用戶

### 🔧 選項 4: curl (命令行)
- **優點**: 無需安裝、腳本友好
- **適合**: 習慣命令行的用戶
- **已內建**: macOS/Linux 都有

### 🌐 選項 5: 瀏覽器 (僅限 GET 請求)
- **適合**: 簡單的 GET 請求測試
- **限制**: 無法測試 POST/PUT/DELETE

---

## 啟動應用

### 方式 1: 直接運行 JAR (推薦)
```bash
# 1. 載入環境變數
source .env.local

# 2. 啟動應用
java -jar target/lunch-selector-1.0.0.jar

# 應該看到:
# Started LunchSelectorApplication in X seconds
```

### 方式 2: 使用 Maven
```bash
source .env.local
mvn spring-boot:run
```

### 方式 3: 使用 Docker
```bash
# 1. 停止舊容器
docker stop lunch-selector 2>/dev/null
docker rm lunch-selector 2>/dev/null

# 2. 啟動新容器
docker run -d \
  --name lunch-selector \
  -p 8080:8080 \
  --env-file .env.local \
  -v $(pwd)/firestore-key.json:/app/firestore-key.json \
  lunch-selector:latest
```

### 驗證應用已啟動
```bash
# 檢查應用是否在運行
curl http://localhost:8080/actuator/health

# 或者用瀏覽器打開:
# http://localhost:8080/
```

---

## 測試步驟

### 測試用戶 ID
```
TEST_USER_001
```

### 基礎 URL
```
http://localhost:8080
```

---

## 📝 完整測試流程

### ✅ 步驟 1: 新增餐廳 (CREATE)

**目標**: 新增 3 家餐廳

#### 1.1 新增麥當勞
```http
POST http://localhost:8080/api/users/TEST_USER_001/restaurants
Content-Type: application/json

{
  "name": "麥當勞",
  "category": "速食",
  "tags": ["快速", "便宜"],
  "rating": 4,
  "notes": "薯條很好吃"
}
```

**預期結果**:
- HTTP 201 Created
- 返回包含 `id` 的餐廳資料
- `visitCount` 應該是 0

**記下 `id`**: _________________ (等等會用到)

---

#### 1.2 新增鼎泰豐
```http
POST http://localhost:8080/api/users/TEST_USER_001/restaurants
Content-Type: application/json

{
  "name": "鼎泰豐",
  "category": "中式",
  "tags": ["好吃", "貴"],
  "rating": 5,
  "notes": "小籠包超讚"
}
```

**記下 `id`**: _________________

---

#### 1.3 新增星巴克
```http
POST http://localhost:8080/api/users/TEST_USER_001/restaurants
Content-Type: application/json

{
  "name": "星巴克",
  "category": "咖啡",
  "tags": ["快速", "中等價位"],
  "rating": 4
}
```

**記下 `id`**: _________________

---

### ✅ 步驟 2: 取得餐廳清單 (READ)

#### 2.1 取得所有餐廳
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants
```

**預期結果**:
- 返回 3 家餐廳
- 按名稱排序 (星巴克、麥當勞、鼎泰豐)

---

#### 2.2 按分類篩選 - 速食
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants?category=速食
```

**預期結果**:
- 只返回麥當勞 (1 家)

---

#### 2.3 按標籤篩選 - 快速
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants?tags=快速
```

**預期結果**:
- 返回麥當勞和星巴克 (2 家)

---

#### 2.4 取得單一餐廳
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants/{麥當勞的ID}
```

**替換**: 將 `{麥當勞的ID}` 替換為步驟 1.1 記下的 ID

**預期結果**:
- 返回麥當勞的完整資訊

---

### ✅ 步驟 3: 隨機推薦

#### 3.1 無限制隨機推薦
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants/random
```

**預期結果**:
- 隨機返回 3 家餐廳之一
- 多次測試應該會得到不同結果

---

#### 3.2 指定分類的隨機推薦
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants/random?category=中式
```

**預期結果**:
- 只會推薦鼎泰豐（因為只有它是中式）

---

### ✅ 步驟 4: 記錄造訪

#### 4.1 記錄麥當勞造訪
```http
POST http://localhost:8080/api/users/TEST_USER_001/restaurants/{麥當勞的ID}/visit
```

**預期結果**:
- HTTP 200 OK
- 返回成功訊息

---

#### 4.2 驗證造訪次數
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants/{麥當勞的ID}
```

**預期結果**:
- `visitCount` 變成 1
- `lastVisit` 有時間戳記

---

#### 4.3 再次記錄造訪
```http
POST http://localhost:8080/api/users/TEST_USER_001/restaurants/{麥當勞的ID}/visit
```

然後再查詢一次，`visitCount` 應該變成 2

---

### ✅ 步驟 5: 更新餐廳 (UPDATE)

#### 5.1 更新麥當勞的評分和備註
```http
PUT http://localhost:8080/api/users/TEST_USER_001/restaurants/{麥當勞的ID}
Content-Type: application/json

{
  "rating": 5,
  "notes": "薯條超讚！升級評分"
}
```

**預期結果**:
- HTTP 200 OK
- `rating` 變成 5
- `notes` 更新
- `updatedAt` 時間更新

---

#### 5.2 只更新標籤
```http
PUT http://localhost:8080/api/users/TEST_USER_001/restaurants/{星巴克的ID}
Content-Type: application/json

{
  "tags": ["快速", "中等價位", "WiFi"]
}
```

**預期結果**:
- 標籤增加了 "WiFi"
- 其他資料保持不變

---

### ✅ 步驟 6: 刪除餐廳 (DELETE)

#### 6.1 軟刪除星巴克
```http
DELETE http://localhost:8080/api/users/TEST_USER_001/restaurants/{星巴克的ID}
```

**預期結果**:
- HTTP 200 OK
- 返回成功訊息

---

#### 6.2 驗證刪除結果
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants
```

**預期結果**:
- 只返回 2 家餐廳（麥當勞和鼎泰豐）
- 星巴克不在清單中

---

#### 6.3 嘗試取得已刪除的餐廳
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants/{星巴克的ID}
```

**預期結果**:
- HTTP 404 Not Found
- 錯誤訊息: "餐廳不存在"

---

### ✅ 步驟 7: 測試排除最近造訪的推薦

#### 7.1 先記錄鼎泰豐造訪
```http
POST http://localhost:8080/api/users/TEST_USER_001/restaurants/{鼎泰豐的ID}/visit
```

---

#### 7.2 隨機推薦（排除最近造訪）
```http
GET http://localhost:8080/api/users/TEST_USER_001/restaurants/random?excludeRecent=true
```

**預期結果**:
- 應該只推薦麥當勞（因為鼎泰豐剛造訪過，星巴克已刪除）

---

## 🎯 測試檢查清單

使用這個清單追蹤你的測試進度：

- [ ] **CREATE**: 成功新增 3 家餐廳
  - [ ] 麥當勞（速食）
  - [ ] 鼎泰豐（中式）
  - [ ] 星巴克（咖啡）

- [ ] **READ**: 成功查詢餐廳
  - [ ] 取得所有餐廳（3 家）
  - [ ] 按分類篩選（速食 → 1 家）
  - [ ] 按標籤篩選（快速 → 2 家）
  - [ ] 取得單一餐廳詳情

- [ ] **UPDATE**: 成功更新餐廳
  - [ ] 更新評分和備註
  - [ ] 只更新標籤（部分更新）

- [ ] **DELETE**: 成功刪除餐廳
  - [ ] 軟刪除星巴克
  - [ ] 驗證刪除後只剩 2 家

- [ ] **進階功能**:
  - [ ] 隨機推薦
  - [ ] 記錄造訪（visitCount 增加）
  - [ ] 排除最近造訪的推薦

---

## 🛠️ Postman 使用指南

### 1. 創建新的 Collection
1. 打開 Postman
2. 點擊 "New" → "Collection"
3. 命名為 "Lunch Selector API"

### 2. 設定環境變數
1. 點擊右上角的齒輪圖示 → "Manage Environments"
2. 點擊 "Add"
3. 設定變數:
   ```
   base_url = http://localhost:8080
   user_id = TEST_USER_001
   ```

### 3. 創建請求
1. 點擊 "New" → "Request"
2. 命名為 "新增餐廳 - 麥當勞"
3. 方法: POST
4. URL: `{{base_url}}/api/users/{{user_id}}/restaurants`
5. Headers:
   - Key: `Content-Type`
   - Value: `application/json`
6. Body → raw → JSON:
   ```json
   {
     "name": "麥當勞",
     "category": "速食",
     "tags": ["快速", "便宜"],
     "rating": 4,
     "notes": "薯條很好吃"
   }
   ```
7. 點擊 "Send"

### 4. 保存響應中的 ID
1. 發送請求後，在響應中找到 `"id": "xxxxx"`
2. 複製這個 ID
3. 在環境變數中新增: `mcdonald_id = xxxxx`
4. 之後可以使用 `{{mcdonald_id}}` 引用

---

## 🐛 常見問題排除

### 問題 1: Connection refused (連線被拒絕)
**原因**: 應用沒有啟動
**解決**:
```bash
source .env.local
java -jar target/lunch-selector-1.0.0.jar
```

### 問題 2: 404 Not Found on /actuator/health
**原因**: Actuator 可能沒有啟用或路徑錯誤
**解決**: 直接訪問 API 端點測試
```bash
curl http://localhost:8080/api/users/TEST_USER_001/restaurants
```

### 問題 3: 400 Bad Request - 驗證失敗
**原因**: 請求資料格式錯誤
**檢查**:
- Content-Type 是否為 `application/json`
- JSON 格式是否正確（逗號、引號）
- 必填欄位 `name` 是否提供

### 問題 4: 500 Internal Server Error
**原因**: 伺服器錯誤
**排查**:
```bash
# 查看應用日誌
tail -f app.log

# 或檢查 Docker 日誌
docker logs lunch-selector
```

### 問題 5: 中文顯示亂碼
**Postman 解決**: Settings → General → "Send UTF-8 header" 打勾

**curl 解決**:
```bash
curl -X POST ... | python3 -m json.tool
```

---

## 📊 測試結果範例

### 成功響應
```json
{
  "success": true,
  "message": "成功新增餐廳",
  "data": {
    "id": "3bc15f09-33a9-40d3-ad2f-f7372a5e34bc",
    "name": "麥當勞",
    "category": "速食",
    "tags": ["快速", "便宜"],
    "rating": 4,
    "notes": "薯條很好吃",
    "visitCount": 0,
    "lastVisit": null,
    "createdAt": "2025-11-12T01:53:16.789",
    "updatedAt": "2025-11-12T01:53:16.789"
  },
  "timestamp": "2025-11-12T01:53:16.903218"
}
```

### 錯誤響應 (404)
```json
{
  "success": false,
  "message": "餐廳不存在: rest-999",
  "data": null,
  "timestamp": "2025-11-12T01:00:00"
}
```

### 錯誤響應 (400 驗證失敗)
```json
{
  "success": false,
  "message": "驗證失敗",
  "data": {
    "name": "餐廳名稱不可為空",
    "rating": "評分最大為 5"
  },
  "timestamp": "2025-11-12T01:00:00"
}
```

---

## 💡 測試技巧

### 1. 使用瀏覽器測試 GET 請求
最簡單的方式！直接在瀏覽器輸入：
```
http://localhost:8080/api/users/TEST_USER_001/restaurants
```

### 2. 使用 curl 快速測試
```bash
# GET 請求
curl http://localhost:8080/api/users/TEST_USER_001/restaurants

# POST 請求
curl -X POST http://localhost:8080/api/users/TEST_USER_001/restaurants \
  -H "Content-Type: application/json" \
  -d '{"name":"測試餐廳","category":"其他","rating":3}'
```

### 3. 保存測試腳本
創建 `test_api.sh`:
```bash
#!/bin/bash
BASE_URL="http://localhost:8080/api/users/TEST_USER_001/restaurants"

echo "=== 1. 新增餐廳 ==="
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"name":"麥當勞","category":"速食","rating":4}'

echo -e "\n\n=== 2. 查詢餐廳 ==="
curl $BASE_URL

echo -e "\n\n=== 3. 隨機推薦 ==="
curl $BASE_URL/random
```

然後執行:
```bash
chmod +x test_api.sh
./test_api.sh
```

---

## 🎓 下一步

完成測試後，你可以：

1. **嘗試錯誤情況**:
   - 提供無效的 rating (6 或 0)
   - 提供空的 name
   - 嘗試更新不存在的餐廳

2. **查看 Firestore Console**:
   - 登入 GCP Console
   - 查看 `user_restaurants` Collection
   - 觀察資料變化

3. **整合到 LINE Bot**:
   - 在 Bot 中添加指令來呼叫這些 API
   - 例如: `/add 麥當勞` → 呼叫 POST API

4. **開發前端 LIFF 應用**:
   - 使用這些 API 創建網頁界面
   - 讓用戶可以視覺化管理餐廳清單

---

**祝測試順利！如果遇到問題，隨時提問。** 🚀
