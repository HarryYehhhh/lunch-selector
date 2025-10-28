package com.lunch.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LINE 用戶實體
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * LINE User ID（也作為 Firestore Document ID）
     */
    private String userId;

    /**
     * 用戶顯示名稱（選填）
     */
    private String displayName;

    /**
     * 註冊時間
     */
    private Timestamp registeredAt;

    /**
     * 最後活躍時間
     */
    private Timestamp lastActiveAt;

    /**
     * 用戶偏好設定
     */
    @Builder.Default
    private UserPreferences preferences = new UserPreferences();

    /**
     * 是否為活躍用戶
     */
    @Builder.Default
    private Boolean active = true;
}
