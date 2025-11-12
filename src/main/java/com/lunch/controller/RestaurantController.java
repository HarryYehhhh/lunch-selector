package com.lunch.controller;

import com.lunch.dto.AddRestaurantRequest;
import com.lunch.dto.ApiResponse;
import com.lunch.dto.RestaurantResponse;
import com.lunch.dto.UpdateRestaurantRequest;
import com.lunch.service.UserRestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 餐廳管理 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/users/{userId}/restaurants")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*") // 暫時允許所有來源，後續會在配置中統一設定
public class RestaurantController {

    private final UserRestaurantService restaurantService;

    /**
     * 取得用戶的所有餐廳
     * GET /api/users/{userId}/restaurants
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getAllRestaurants(
            @PathVariable String userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags) {

        log.info("📋 取得餐廳清單: userId={}, category={}, tags={}", userId, category, tags);

        List<RestaurantResponse> restaurants = restaurantService.getUserRestaurants(userId, category, tags);

        return ResponseEntity.ok(
                ApiResponse.success("成功取得餐廳清單", restaurants)
        );
    }

    /**
     * 取得單一餐廳
     * GET /api/users/{userId}/restaurants/{restaurantId}
     */
    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant(
            @PathVariable String userId,
            @PathVariable String restaurantId) {

        log.info("🔍 取得餐廳: userId={}, restaurantId={}", userId, restaurantId);

        RestaurantResponse restaurant = restaurantService.getRestaurant(userId, restaurantId);

        return ResponseEntity.ok(
                ApiResponse.success("成功取得餐廳", restaurant)
        );
    }

    /**
     * 新增餐廳
     * POST /api/users/{userId}/restaurants
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantResponse>> addRestaurant(
            @PathVariable String userId,
            @Valid @RequestBody AddRestaurantRequest request) {

        log.info("➕ 新增餐廳: userId={}, name={}", userId, request.getName());

        RestaurantResponse restaurant = restaurantService.addRestaurant(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("成功新增餐廳", restaurant));
    }

    /**
     * 更新餐廳
     * PUT /api/users/{userId}/restaurants/{restaurantId}
     */
    @PutMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable String userId,
            @PathVariable String restaurantId,
            @Valid @RequestBody UpdateRestaurantRequest request) {

        log.info("✏️ 更新餐廳: userId={}, restaurantId={}", userId, restaurantId);

        RestaurantResponse restaurant = restaurantService.updateRestaurant(userId, restaurantId, request);

        return ResponseEntity.ok(
                ApiResponse.success("成功更新餐廳", restaurant)
        );
    }

    /**
     * 刪除餐廳
     * DELETE /api/users/{userId}/restaurants/{restaurantId}
     */
    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<Void>> deleteRestaurant(
            @PathVariable String userId,
            @PathVariable String restaurantId) {

        log.info("🗑️ 刪除餐廳: userId={}, restaurantId={}", userId, restaurantId);

        restaurantService.deleteRestaurant(userId, restaurantId);

        return ResponseEntity.ok(
                ApiResponse.success("成功刪除餐廳", null)
        );
    }

    /**
     * 隨機推薦餐廳
     * GET /api/users/{userId}/restaurants/random
     */
    @GetMapping("/random")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRandomRestaurant(
            @PathVariable String userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false, defaultValue = "false") boolean excludeRecent) {

        log.info("🎲 隨機推薦: userId={}, category={}, tags={}, excludeRecent={}",
                userId, category, tags, excludeRecent);

        RestaurantResponse restaurant = restaurantService.getRandomRestaurant(
                userId, category, tags, excludeRecent
        );

        return ResponseEntity.ok(
                ApiResponse.success("隨機推薦餐廳", restaurant)
        );
    }

    /**
     * 記錄餐廳造訪
     * POST /api/users/{userId}/restaurants/{restaurantId}/visit
     */
    @PostMapping("/{restaurantId}/visit")
    public ResponseEntity<ApiResponse<Void>> recordVisit(
            @PathVariable String userId,
            @PathVariable String restaurantId) {

        log.info("📝 記錄造訪: userId={}, restaurantId={}", userId, restaurantId);

        restaurantService.recordVisit(userId, restaurantId);

        return ResponseEntity.ok(
                ApiResponse.success("成功記錄造訪", null)
        );
    }
}
