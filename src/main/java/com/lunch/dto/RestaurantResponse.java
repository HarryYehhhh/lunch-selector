package com.lunch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 餐廳響應 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {

    /**
     * 餐廳 ID
     */
    private String id;

    /**
     * 餐廳名稱
     */
    private String name;

    /**
     * 分類
     */
    private String category;

    /**
     * 標籤
     */
    private List<String> tags;

    /**
     * 評分
     */
    private Integer rating;

    /**
     * 備註
     */
    private String notes;

    /**
     * 造訪次數
     */
    private Integer visitCount;

    /**
     * 上次造訪時間
     */
    private LocalDateTime lastVisit;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;
}
