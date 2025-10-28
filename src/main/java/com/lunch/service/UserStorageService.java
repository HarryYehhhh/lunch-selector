package com.lunch.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.lunch.model.User;
import com.lunch.model.UserPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * 用戶存儲服務（使用 Firestore）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStorageService {

    private static final String COLLECTION_NAME = "users";

    private final Firestore firestore;

    /**
     * 添加新用戶（如果不存在）
     *
     * @param userId LINE 用戶 ID
     * @return true 如果是新用戶，false 如果已存在
     */
    public boolean addUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("⚠️ 嘗試添加空的用戶 ID");
            return false;
        }

        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
            DocumentSnapshot document = docRef.get().get();

            if (document.exists()) {
                // 用戶已存在，更新最後活躍時間
                docRef.update("lastActiveAt", Timestamp.now()).get();
                log.debug("ℹ️ 用戶已存在，已更新活躍時間: {}", userId);
                return false;
            } else {
                // 創建新用戶
                User newUser = User.builder()
                        .userId(userId)
                        .registeredAt(Timestamp.now())
                        .lastActiveAt(Timestamp.now())
                        .preferences(UserPreferences.builder().build())
                        .active(true)
                        .build();

                docRef.set(newUser).get();
                log.info("✅ 新用戶已註冊: {} (總用戶數: {})", userId, getUserCount());
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 添加用戶時發生錯誤: {}", userId, e);
            return false;
        }
    }

    /**
     * 獲取用戶資訊
     *
     * @param userId LINE 用戶 ID
     * @return Optional<User>
     */
    public Optional<User> getUser(String userId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
            DocumentSnapshot document = docRef.get().get();

            if (document.exists()) {
                User user = document.toObject(User.class);
                return Optional.ofNullable(user);
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 獲取用戶時發生錯誤: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * 獲取所有活躍用戶
     *
     * @return 用戶列表
     */
    public List<User> getAllActiveUsers() {
        try {
            CollectionReference usersRef = firestore.collection(COLLECTION_NAME);
            Query query = usersRef.whereEqualTo("active", true);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();

            List<User> users = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                User user = document.toObject(User.class);
                if (user != null) {
                    users.add(user);
                }
            }

            return users;
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 獲取用戶列表時發生錯誤", e);
            return new ArrayList<>();
        }
    }

    /**
     * 獲取所有啟用通知的用戶
     *
     * @return 用戶列表
     */
    public List<User> getNotificationEnabledUsers() {
        try {
            CollectionReference usersRef = firestore.collection(COLLECTION_NAME);
            Query query = usersRef
                    .whereEqualTo("active", true)
                    .whereEqualTo("preferences.notificationEnabled", true);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();

            List<User> users = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                User user = document.toObject(User.class);
                if (user != null) {
                    users.add(user);
                }
            }

            log.debug("📊 找到 {} 位啟用通知的用戶", users.size());
            return users;
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 獲取啟用通知的用戶時發生錯誤", e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新用戶偏好設定
     *
     * @param userId      LINE 用戶 ID
     * @param preferences 新的偏好設定
     * @return true 如果成功，false 如果失敗
     */
    public boolean updateUserPreferences(String userId, UserPreferences preferences) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);

            // 檢查用戶是否存在
            if (!docRef.get().get().exists()) {
                log.warn("⚠️ 用戶不存在: {}", userId);
                return false;
            }

            docRef.update(
                    "preferences", preferences,
                    "lastActiveAt", Timestamp.now()
            ).get();

            log.info("✅ 已更新用戶偏好: {}", userId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 更新用戶偏好時發生錯誤: {}", userId, e);
            return false;
        }
    }

    /**
     * 移除用戶（設為非活躍）
     *
     * @param userId LINE 用戶 ID
     * @return true 如果成功移除，false 如果用戶不存在
     */
    public boolean removeUser(String userId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);

            if (!docRef.get().get().exists()) {
                log.debug("ℹ️ 用戶不存在: {}", userId);
                return false;
            }

            // 設為非活躍而不是直接刪除（保留歷史記錄）
            docRef.update("active", false).get();

            log.info("🗑️ 用戶已設為非活躍: {}", userId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 移除用戶時發生錯誤: {}", userId, e);
            return false;
        }
    }

    /**
     * 檢查用戶是否存在
     *
     * @param userId LINE 用戶 ID
     * @return true 如果存在，false 如果不存在
     */
    public boolean hasUser(String userId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
            return docRef.get().get().exists();
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 檢查用戶是否存在時發生錯誤: {}", userId, e);
            return false;
        }
    }

    /**
     * 獲取活躍用戶總數
     *
     * @return 用戶數量
     */
    public int getUserCount() {
        try {
            CollectionReference usersRef = firestore.collection(COLLECTION_NAME);
            Query query = usersRef.whereEqualTo("active", true);
            return query.get().get().size();
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 獲取用戶數量時發生錯誤", e);
            return 0;
        }
    }

    /**
     * 啟用用戶通知
     *
     * @param userId LINE 用戶 ID
     * @return true 如果成功
     */
    public boolean enableNotification(String userId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);

            if (!docRef.get().get().exists()) {
                log.warn("⚠️ 用戶不存在: {}", userId);
                return false;
            }

            docRef.update("preferences.notificationEnabled", true).get();
            log.info("🔔 已啟用用戶通知: {}", userId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 啟用通知時發生錯誤: {}", userId, e);
            return false;
        }
    }

    /**
     * 關閉用戶通知
     *
     * @param userId LINE 用戶 ID
     * @return true 如果成功
     */
    public boolean disableNotification(String userId) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);

            if (!docRef.get().get().exists()) {
                log.warn("⚠️ 用戶不存在: {}", userId);
                return false;
            }

            docRef.update("preferences.notificationEnabled", false).get();
            log.info("🔕 已關閉用戶通知: {}", userId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 關閉通知時發生錯誤: {}", userId, e);
            return false;
        }
    }

    /**
     * 設定用戶偏好的餐廳列表
     *
     * @param userId      LINE 用戶 ID
     * @param restaurants 餐廳列表
     * @return true 如果成功
     */
    public boolean setUserRestaurants(String userId, List<String> restaurants) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);

            if (!docRef.get().get().exists()) {
                log.warn("⚠️ 用戶不存在: {}", userId);
                return false;
            }

            docRef.update(
                    "preferences.restaurants", restaurants,
                    "lastActiveAt", Timestamp.now()
            ).get();

            log.info("🍽️ 已更新用戶餐廳偏好: {} (數量: {})", userId, restaurants.size());
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("❌ 設定餐廳偏好時發生錯誤: {}", userId, e);
            return false;
        }
    }
}
