package com.lunch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

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
}
