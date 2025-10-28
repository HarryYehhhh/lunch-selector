package com.lunch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunch.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LunchService {

    private List<String> restaurants;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("restaurants.json");
            restaurants = objectMapper.readValue(
                resource.getInputStream(), 
                new TypeReference<List<String>>() {}
            );
            log.info("載入 {} 家餐廳", restaurants.size());
        } catch (IOException e) {
            log.error("無法載入餐廳清單", e);
            restaurants = List.of(
                "麥當勞 🍔",
                "便當店 🍱",
                "麵店 🍜",
                "自助餐 🍛",
                "滷肉飯 🍚"
            );
        }
    }
    
    public String selectRandomRestaurant() {
        if (restaurants.isEmpty()) {
            return "今天自己決定吧！";
        }
        return restaurants.get(random.nextInt(restaurants.size()));
    }
    
    /**
     * 根據用戶偏好選擇餐廳
     *
     * @param user 用戶物件（包含偏好設定）
     * @return 推薦的餐廳
     */
    public String selectRestaurantForUser(User user) {
        if (user == null || user.getPreferences() == null) {
            return selectRandomRestaurant();
        }

        List<String> userRestaurants = user.getPreferences().getRestaurants();
        List<String> excludeRestaurants = user.getPreferences().getExcludeRestaurants();

        // 如果用戶有設定偏好餐廳列表
        if (userRestaurants != null && !userRestaurants.isEmpty()) {
            // 從偏好列表中排除不要的餐廳
            List<String> filteredRestaurants = userRestaurants.stream()
                    .filter(r -> excludeRestaurants == null || !excludeRestaurants.contains(r))
                    .collect(Collectors.toList());

            if (filteredRestaurants.isEmpty()) {
                log.warn("⚠️ 用戶 {} 的偏好餐廳被全部排除，使用預設列表", user.getUserId());
                return selectRandomRestaurant();
            }

            int index = random.nextInt(filteredRestaurants.size());
            return filteredRestaurants.get(index);
        }

        // 如果沒有設定偏好，使用全部餐廳但排除不要的
        if (excludeRestaurants != null && !excludeRestaurants.isEmpty()) {
            List<String> filteredRestaurants = restaurants.stream()
                    .filter(r -> !excludeRestaurants.contains(r))
                    .collect(Collectors.toList());

            if (filteredRestaurants.isEmpty()) {
                log.warn("⚠️ 用戶 {} 排除了所有餐廳，使用預設列表", user.getUserId());
                return selectRandomRestaurant();
            }

            int index = random.nextInt(filteredRestaurants.size());
            return filteredRestaurants.get(index);
        }

        // 使用全部餐廳
        return selectRandomRestaurant();
    }

    public String formatLunchMessage(String restaurant) {
        String date = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd (E)"));

        return String.format(
            "🍽️ 今日午餐建議 🍽️\n\n" +
            "📅 日期：%s\n" +
            "🎯 推薦：%s\n\n" +
            "祝用餐愉快！😋",
            date, restaurant
        );
    }

    /**
     * 格式化個性化午餐訊息
     *
     * @param restaurant 餐廳名稱
     * @param user       用戶物件
     * @return 格式化的訊息
     */
    public String formatLunchMessageForUser(String restaurant, User user) {
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd (E)"));

        // 如果用戶有自訂訊息，使用自訂格式
        if (user != null && user.getPreferences() != null &&
            user.getPreferences().getCustomMessage() != null &&
            !user.getPreferences().getCustomMessage().isEmpty()) {

            return String.format(
                    "%s\n\n" +
                    "📅 日期：%s\n" +
                    "🎯 推薦：%s\n\n" +
                    "祝用餐愉快！😋",
                    user.getPreferences().getCustomMessage(), date, restaurant
            );
        }

        // 使用預設格式
        return formatLunchMessage(restaurant);
    }

    /**
     * 獲取所有可用餐廳列表
     *
     * @return 餐廳列表
     */
    public List<String> getAllRestaurants() {
        return new ArrayList<>(restaurants);
    }
}
