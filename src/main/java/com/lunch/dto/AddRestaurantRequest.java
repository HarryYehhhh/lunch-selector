package com.lunch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 新增餐廳請求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRestaurantRequest {

    /**
     * 餐廳名稱（必填）
     */
    @NotBlank(message = "餐廳名稱不可為空")
    @Size(max = 100, message = "餐廳名稱長度不可超過 100 字元")
    private String name;

    /**
     * 分類（選填）
     */
    @Size(max = 50, message = "分類長度不可超過 50 字元")
    private String category;

    /**
     * 標籤（選填）
     */
    private List<String> tags;

    /**
     * 評分（選填，1-5 星）
     */
    @Min(value = 1, message = "評分最小為 1")
    @Max(value = 5, message = "評分最大為 5")
    private Integer rating;

    /**
     * 備註（選填）
     */
    @Size(max = 500, message = "備註長度不可超過 500 字元")
    private String notes;
}
