# 🍽️ 餐廳管理 API 文檔

## 📋 目錄
- [API 端點總覽](#api-端點總覽)
- [資料模型](#資料模型)
- [API 詳細說明](#api-詳細說明)
- [使用範例](#使用範例)
- [錯誤處理](#錯誤處理)

---

## API 端點總覽

| 方法 | 端點 | 說明 |
|------|------|------|
| GET | `/api/users/{userId}/restaurants` | 取得用戶的所有餐廳 |
| GET | `/api/users/{userId}/restaurants/{restaurantId}` | 取得單一餐廳 |
| POST | `/api/users/{userId}/restaurants` | 新增餐廳 |
| PUT | `/api/users/{userId}/restaurants/{restaurantId}` | 更新餐廳 |
| DELETE | `/api/users/{userId}/restaurants/{restaurantId}` | 刪除餐廳 |
| GET | `/api/users/{userId}/restaurants/random` | 隨機推薦餐廳 |
| POST | `/api/users/{userId}/restaurants/{restaurantId}/visit` | 記錄餐廳造訪 |

---

## 資料模型

### RestaurantResponse
```json
{
  "id": "string",
  "name": "string",
  "category": "string",
  "tags": ["string"],
  "rating": 1-5,
  "notes": "string",
  "visitCount": 0,
  "lastVisit": "2025-11-12T01:00:00",
  "createdAt": "2025-11-12T01:00:00",
  "updatedAt": "2025-11-12T01:00:00"
}
```

### ApiResponse
```json
{
  "success": true,
  "message": "string",
  "data": {},
  "timestamp": "2025-11-12T01:00:00"
}
```

---

## API 詳細說明

### 1. 取得用戶的所有餐廳

**請求**
```http
GET /api/users/{userId}/restaurants?category=中式&tags=快速,便宜
```

**參數**
- `userId` (path) - LINE 用戶 ID
- `category` (query, optional) - 分類篩選
- `tags` (query, optional) - 標籤篩選（可多個）

**響應**
```json
{
  "success": true,
  "message": "成功取得餐廳清單",
  "data": [
    {
      "id": "rest-001",
      "name": "麥當勞",
      "category": "速食",
      "tags": ["快速", "便宜"],
      "rating": 4,
      "notes": "薯條很好吃",
      "visitCount": 5,
      "lastVisit": "2025-11-10T12:00:00",
      "createdAt": "2025-11-01T10:00:00",
      "updatedAt": "2025-11-10T12:00:00"
    }
  ],
  "timestamp": "2025-11-12T01:00:00"
}
```

---

### 2. 新增餐廳

**請求**
```http
POST /api/users/{userId}/restaurants
Content-Type: application/json

{
  "name": "鼎泰豐",
  "category": "中式",
  "tags": ["好吃", "貴"],
  "rating": 5,
  "notes": "小籠包超讚"
}
```

**欄位驗證**
- `name`: 必填，最大 100 字元
- `category`: 選填，最大 50 字元
- `tags`: 選填，字串陣列
- `rating`: 選填，1-5 之間
- `notes`: 選填，最大 500 字元

**響應**
```json
{
  "success": true,
  "message": "成功新增餐廳",
  "data": {
    "id": "rest-002",
    "name": "鼎泰豐",
    "category": "中式",
    "tags": ["好吃", "貴"],
    "rating": 5,
    "notes": "小籠包超讚",
    "visitCount": 0,
    "lastVisit": null,
    "createdAt": "2025-11-12T01:00:00",
    "updatedAt": "2025-11-12T01:00:00"
  },
  "timestamp": "2025-11-12T01:00:00"
}
```

**狀態碼**: `201 Created`

---

### 3. 更新餐廳

**請求**
```http
PUT /api/users/{userId}/restaurants/{restaurantId}
Content-Type: application/json

{
  "rating": 4,
  "notes": "更新後的備註"
}
```

**注意**: 所有欄位都是選填，只更新提供的欄位

**響應**
```json
{
  "success": true,
  "message": "成功更新餐廳",
  "data": {
    "id": "rest-002",
    "name": "鼎泰豐",
    "rating": 4,
    "notes": "更新後的備註",
    ...
  },
  "timestamp": "2025-11-12T01:00:00"
}
```

---

### 4. 刪除餐廳

**請求**
```http
DELETE /api/users/{userId}/restaurants/{restaurantId}
```

**響應**
```json
{
  "success": true,
  "message": "成功刪除餐廳",
  "data": null,
  "timestamp": "2025-11-12T01:00:00"
}
```

**注意**: 這是軟刪除，資料仍保留在資料庫中

---

### 5. 隨機推薦餐廳

**請求**
```http
GET /api/users/{userId}/restaurants/random?category=中式&excludeRecent=true
```

**參數**
- `category` (query, optional) - 分類篩選
- `tags` (query, optional) - 標籤篩選
- `excludeRecent` (query, optional, default: false) - 排除 7 天內造訪過的

**響應**
```json
{
  "success": true,
  "message": "隨機推薦餐廳",
  "data": {
    "id": "rest-003",
    "name": "星巴克",
    "category": "咖啡",
    ...
  },
  "timestamp": "2025-11-12T01:00:00"
}
```

---

### 6. 記錄餐廳造訪

**請求**
```http
POST /api/users/{userId}/restaurants/{restaurantId}/visit
```

**響應**
```json
{
  "success": true,
  "message": "成功記錄造訪",
  "data": null,
  "timestamp": "2025-11-12T01:00:00"
}
```

**效果**:
- `visitCount` +1
- `lastVisit` 更新為當前時間

---

## 使用範例

### 使用 curl 測試

```bash
# 1. 新增餐廳
curl -X POST http://localhost:8080/api/users/U123/restaurants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "麥當勞",
    "category": "速食",
    "tags": ["快速", "便宜"],
    "rating": 4
  }'

# 2. 取得所有餐廳
curl http://localhost:8080/api/users/U123/restaurants

# 3. 隨機推薦（排除最近吃過的）
curl "http://localhost:8080/api/users/U123/restaurants/random?excludeRecent=true"

# 4. 記錄造訪
curl -X POST http://localhost:8080/api/users/U123/restaurants/rest-001/visit

# 5. 更新餐廳
curl -X PUT http://localhost:8080/api/users/U123/restaurants/rest-001 \
  -H "Content-Type: application/json" \
  -d '{"rating": 5}'

# 6. 刪除餐廳
curl -X DELETE http://localhost:8080/api/users/U123/restaurants/rest-001
```

### 使用 JavaScript (Fetch API)

```javascript
// 新增餐廳
async function addRestaurant(userId, data) {
  const response = await fetch(`/api/users/${userId}/restaurants`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  });
  return response.json();
}

// 取得所有餐廳
async function getRestaurants(userId, filters = {}) {
  const params = new URLSearchParams(filters);
  const response = await fetch(`/api/users/${userId}/restaurants?${params}`);
  return response.json();
}

// 隨機推薦
async function getRandomRestaurant(userId) {
  const response = await fetch(
    `/api/users/${userId}/restaurants/random?excludeRecent=true`
  );
  return response.json();
}

// 使用範例
const result = await addRestaurant('U123', {
  name: '鼎泰豐',
  category: '中式',
  rating: 5
});

console.log(result);
```

---

## 錯誤處理

### 400 Bad Request - 驗證失敗
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

### 404 Not Found - 餐廳不存在
```json
{
  "success": false,
  "message": "餐廳不存在: rest-999",
  "data": null,
  "timestamp": "2025-11-12T01:00:00"
}
```

### 500 Internal Server Error - 系統錯誤
```json
{
  "success": false,
  "message": "系統錯誤: Connection timeout",
  "data": null,
  "timestamp": "2025-11-12T01:00:00"
}
```

---

## Firestore 資料結構

```
user_restaurants (Collection)
  └── {restaurantId} (Document)
      ├── userId: "U1aec69f..."
      ├── name: "麥當勞"
      ├── category: "速食"
      ├── tags: ["快速", "便宜"]
      ├── rating: 4
      ├── notes: "薯條很好吃"
      ├── visitCount: 5
      ├── lastVisit: Timestamp
      ├── createdAt: Timestamp
      ├── updatedAt: Timestamp
      └── active: true
```

---

## 下一步

- [ ] 撰寫單元測試
- [ ] 添加身份驗證 (LINE Access Token)
- [ ] 實作前端 LIFF 應用
- [ ] 添加餐廳圖片上傳功能
- [ ] 實作餐廳分享功能
- [ ] 添加統計報表 API

---

**最後更新**: 2025-11-12
**版本**: 1.0.0
