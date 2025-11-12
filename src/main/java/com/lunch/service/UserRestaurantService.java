package com.lunch.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.lunch.dto.AddRestaurantRequest;
import com.lunch.dto.RestaurantResponse;
import com.lunch.dto.UpdateRestaurantRequest;
import com.lunch.exception.RestaurantNotFoundException;
import com.lunch.model.UserRestaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 用戶餐廳服務
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRestaurantService {

    private static final String COLLECTION_NAME = "user_restaurants";
    private final Firestore firestore;

    /**
     * 取得用戶的所有餐廳
     *
     * @param userId   用戶 ID
     * @param category 分類篩選（可選）
     * @param tags     標籤篩選（可選）
     * @return 餐廳列表
     */
    public List<RestaurantResponse> getUserRestaurants(String userId, String category, List<String> tags) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("active", true);

            // 分類篩選
            if (category != null && !category.trim().isEmpty()) {
                query = query.whereEqualTo("category", category);
            }

            // 標籤篩選（包含任一標籤）
            if (tags != null && !tags.isEmpty()) {
                query = query.whereArrayContainsAny("tags", tags);
            }

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            log.info("✅ 取得用戶 {} 的餐廳清單，共 {} 筆", userId, documents.size());

            return documents.stream()
                    .map(this::documentToResponse)
                    .sorted(Comparator.comparing(RestaurantResponse::getName))
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 取得餐廳清單失敗: userId={}", userId, e);
            throw new RuntimeException("取得餐廳清單失敗", e);
        }
    }

    /**
     * 取得單一餐廳
     *
     * @param userId       用戶 ID
     * @param restaurantId 餐廳 ID
     * @return 餐廳資訊
     */
    public RestaurantResponse getRestaurant(String userId, String restaurantId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(restaurantId);
            DocumentSnapshot document = docRef.get().get();

            if (!document.exists()) {
                throw new RestaurantNotFoundException("餐廳不存在: " + restaurantId);
            }

            UserRestaurant restaurant = document.toObject(UserRestaurant.class);

            // 驗證是否屬於該用戶
            if (restaurant != null && !restaurant.getUserId().equals(userId)) {
                throw new RestaurantNotFoundException("無權訪問此餐廳");
            }

            if (restaurant == null || !restaurant.getActive()) {
                throw new RestaurantNotFoundException("餐廳不存在: " + restaurantId);
            }

            log.info("✅ 取得餐廳: {}", restaurant.getName());
            return toResponse(restaurant);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 取得餐廳失敗: restaurantId={}", restaurantId, e);
            throw new RuntimeException("取得餐廳失敗", e);
        }
    }

    /**
     * 新增餐廳
     *
     * @param userId  用戶 ID
     * @param request 新增請求
     * @return 新增的餐廳
     */
    public RestaurantResponse addRestaurant(String userId, AddRestaurantRequest request) {
        try {
            // 檢查是否已存在同名餐廳
            if (isRestaurantExists(userId, request.getName())) {
                log.warn("⚠️ 餐廳已存在: {}", request.getName());
                throw new RuntimeException("餐廳已存在: " + request.getName());
            }

            UserRestaurant restaurant = UserRestaurant.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .name(request.getName().trim())
                    .category(request.getCategory() != null ? request.getCategory().trim() : null)
                    .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                    .rating(request.getRating())
                    .notes(request.getNotes())
                    .visitCount(0)
                    .createdAt(Timestamp.now())
                    .updatedAt(Timestamp.now())
                    .active(true)
                    .build();

            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(restaurant.getId());
            docRef.set(restaurant).get();

            log.info("✅ 新增餐廳成功: userId={}, name={}", userId, restaurant.getName());

            return toResponse(restaurant);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 新增餐廳失敗: userId={}, name={}", userId, request.getName(), e);
            throw new RuntimeException("新增餐廳失敗", e);
        }
    }

    /**
     * 更新餐廳
     *
     * @param userId       用戶 ID
     * @param restaurantId 餐廳 ID
     * @param request      更新請求
     * @return 更新後的餐廳
     */
    public RestaurantResponse updateRestaurant(String userId, String restaurantId, UpdateRestaurantRequest request) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(restaurantId);
            DocumentSnapshot document = docRef.get().get();

            if (!document.exists()) {
                throw new RestaurantNotFoundException("餐廳不存在: " + restaurantId);
            }

            UserRestaurant restaurant = document.toObject(UserRestaurant.class);

            // 驗證是否屬於該用戶
            if (restaurant != null && !restaurant.getUserId().equals(userId)) {
                throw new RestaurantNotFoundException("無權修改此餐廳");
            }

            // 建立更新 Map
            Map<String, Object> updates = new HashMap<>();

            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                updates.put("name", request.getName().trim());
            }
            if (request.getCategory() != null) {
                updates.put("category", request.getCategory().trim());
            }
            if (request.getTags() != null) {
                updates.put("tags", request.getTags());
            }
            if (request.getRating() != null) {
                updates.put("rating", request.getRating());
            }
            if (request.getNotes() != null) {
                updates.put("notes", request.getNotes());
            }

            updates.put("updatedAt", Timestamp.now());

            if (!updates.isEmpty()) {
                docRef.update(updates).get();
                log.info("✅ 更新餐廳成功: restaurantId={}", restaurantId);
            }

            // 重新取得更新後的資料
            return getRestaurant(userId, restaurantId);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 更新餐廳失敗: restaurantId={}", restaurantId, e);
            throw new RuntimeException("更新餐廳失敗", e);
        }
    }

    /**
     * 刪除餐廳（軟刪除）
     *
     * @param userId       用戶 ID
     * @param restaurantId 餐廳 ID
     */
    public void deleteRestaurant(String userId, String restaurantId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(restaurantId);
            DocumentSnapshot document = docRef.get().get();

            if (!document.exists()) {
                throw new RestaurantNotFoundException("餐廳不存在: " + restaurantId);
            }

            UserRestaurant restaurant = document.toObject(UserRestaurant.class);

            // 驗證是否屬於該用戶
            if (restaurant != null && !restaurant.getUserId().equals(userId)) {
                throw new RestaurantNotFoundException("無權刪除此餐廳");
            }

            // 軟刪除
            docRef.update(
                    "active", false,
                    "updatedAt", Timestamp.now()
            ).get();

            log.info("✅ 刪除餐廳成功: restaurantId={}", restaurantId);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 刪除餐廳失敗: restaurantId={}", restaurantId, e);
            throw new RuntimeException("刪除餐廳失敗", e);
        }
    }

    /**
     * 隨機推薦餐廳
     *
     * @param userId        用戶 ID
     * @param category      分類篩選（可選）
     * @param tags          標籤篩選（可選）
     * @param excludeRecent 是否排除最近造訪的（7天內）
     * @return 隨機推薦的餐廳
     */
    public RestaurantResponse getRandomRestaurant(String userId, String category,
                                                   List<String> tags, boolean excludeRecent) {
        List<RestaurantResponse> restaurants = getUserRestaurants(userId, category, tags);

        if (restaurants.isEmpty()) {
            throw new RestaurantNotFoundException("沒有符合條件的餐廳");
        }

        // 排除最近 7 天造訪過的
        if (excludeRecent) {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            restaurants = restaurants.stream()
                    .filter(r -> r.getLastVisit() == null || r.getLastVisit().isBefore(sevenDaysAgo))
                    .collect(Collectors.toList());

            if (restaurants.isEmpty()) {
                throw new RestaurantNotFoundException("沒有符合條件的餐廳（已排除最近造訪）");
            }
        }

        // 隨機選擇
        Random random = new Random();
        RestaurantResponse selected = restaurants.get(random.nextInt(restaurants.size()));

        log.info("🎲 隨機推薦餐廳: userId={}, restaurant={}", userId, selected.getName());

        return selected;
    }

    /**
     * 記錄餐廳造訪
     *
     * @param userId       用戶 ID
     * @param restaurantId 餐廳 ID
     */
    public void recordVisit(String userId, String restaurantId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(restaurantId);
            DocumentSnapshot document = docRef.get().get();

            if (!document.exists()) {
                throw new RestaurantNotFoundException("餐廳不存在: " + restaurantId);
            }

            UserRestaurant restaurant = document.toObject(UserRestaurant.class);

            // 驗證是否屬於該用戶
            if (restaurant != null && !restaurant.getUserId().equals(userId)) {
                throw new RestaurantNotFoundException("無權操作此餐廳");
            }

            Integer currentCount = restaurant != null ? restaurant.getVisitCount() : 0;

            docRef.update(
                    "visitCount", currentCount + 1,
                    "lastVisit", Timestamp.now(),
                    "updatedAt", Timestamp.now()
            ).get();

            log.info("✅ 記錄造訪: restaurantId={}, visitCount={}", restaurantId, currentCount + 1);

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 記錄造訪失敗: restaurantId={}", restaurantId, e);
            throw new RuntimeException("記錄造訪失敗", e);
        }
    }

    /**
     * 檢查餐廳是否存在
     */
    private boolean isRestaurantExists(String userId, String name) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("name", name.trim())
                    .whereEqualTo("active", true);

            return !query.get().get().isEmpty();

        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 檢查餐廳是否存在失敗", e);
            return false;
        }
    }

    /**
     * 將 Firestore Document 轉換為 Response DTO
     */
    private RestaurantResponse documentToResponse(QueryDocumentSnapshot document) {
        return RestaurantResponse.builder()
                .id(document.getId())
                .name(document.getString("name"))
                .category(document.getString("category"))
                .tags((List<String>) document.get("tags"))
                .rating(document.getLong("rating") != null ? document.getLong("rating").intValue() : null)
                .notes(document.getString("notes"))
                .visitCount(document.getLong("visitCount") != null ?
                        document.getLong("visitCount").intValue() : 0)
                .lastVisit(timestampToLocalDateTime(document.getTimestamp("lastVisit")))
                .createdAt(timestampToLocalDateTime(document.getTimestamp("createdAt")))
                .updatedAt(timestampToLocalDateTime(document.getTimestamp("updatedAt")))
                .build();
    }

    /**
     * 將 UserRestaurant 轉換為 Response DTO
     */
    private RestaurantResponse toResponse(UserRestaurant restaurant) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .category(restaurant.getCategory())
                .tags(restaurant.getTags())
                .rating(restaurant.getRating())
                .notes(restaurant.getNotes())
                .visitCount(restaurant.getVisitCount())
                .lastVisit(timestampToLocalDateTime(restaurant.getLastVisit()))
                .createdAt(timestampToLocalDateTime(restaurant.getCreatedAt()))
                .updatedAt(timestampToLocalDateTime(restaurant.getUpdatedAt()))
                .build();
    }

    /**
     * 將 Firestore Timestamp 轉換為 LocalDateTime
     */
    private LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                timestamp.toDate().toInstant(),
                ZoneId.systemDefault()
        );
    }
}
