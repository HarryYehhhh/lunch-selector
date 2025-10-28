package com.lunch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 用戶偏好設定
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {

    /**
     * 偏好的餐廳列表
     * 空列表 = 使用全部餐廳
     */
    @Builder.Default
    private List<String> restaurants = new ArrayList<>();

    /**
     * 排除的餐廳列表
     */
    @Builder.Default
    private List<String> excludeRestaurants = new ArrayList<>();

    /**
     * 是否啟用通知
     */
    @Builder.Default
    private Boolean notificationEnabled = true;

    /**
     * 自訂通知時間（格式：HH:mm，例如 "11:50"）
     * null = 使用預設時間
     */
    private String notificationTime;

    /**
     * 自訂訊息前綴
     */
    private String customMessage;
}
