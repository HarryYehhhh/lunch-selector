package com.lunch.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 用戶自訂餐廳實體
 * 存儲在 Firestore 的 user_restaurants Collection 中
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRestaurant {

    /**
     * 餐廳 ID（Firestore Document ID）
     */
    private String id;

    /**
     * LINE User ID（用於查詢該用戶的所有餐廳）
     */
    private String userId;

    /**
     * 餐廳名稱
     */
    private String name;

    /**
     * 分類（中式/日式/西式/速食/其他）
     */
    private String category;

    /**
     * 標籤（快速/健康/貴/便宜/好吃/普通）
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * 評分（1-5 星）
     */
    private Integer rating;

    /**
     * 備註
     */
    private String notes;

    /**
     * 造訪次數（用於統計）
     */
    @Builder.Default
    private Integer visitCount = 0;

    /**
     * 上次造訪時間（用於避免重複推薦）
     */
    private Timestamp lastVisit;

    /**
     * 建立時間
     */
    private Timestamp createdAt;

    /**
     * 更新時間
     */
    private Timestamp updatedAt;

    /**
     * 是否啟用（軟刪除）
     */
    @Builder.Default
    private Boolean active = true;
}
