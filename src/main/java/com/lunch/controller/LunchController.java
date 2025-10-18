package com.lunch.controller;

import com.lunch.service.LunchService;
import com.lunch.service.LineMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LunchController {
    
    private final LunchService lunchService;
    private final LineMessagingService lineMessagingService;
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
    
    @GetMapping("/lunch/manual")
    public ResponseEntity<String> selectLunchManual() {
        String restaurant = lunchService.selectRandomRestaurant();
        log.info("手動選擇午餐: {}", restaurant);
        return ResponseEntity.ok("今天午餐：" + restaurant);
    }
    
    @PostMapping("/lunch/notify")
    public ResponseEntity<String> notifyLunch() {
        try {
            String restaurant = lunchService.selectRandomRestaurant();
            String message = lunchService.formatLunchMessage(restaurant);
            
            lineMessagingService.sendLunchNotification(message);
            
            log.info("午餐通知已發送: {}", restaurant);
            return ResponseEntity.ok("Notification sent: " + restaurant);
        } catch (Exception e) {
            log.error("發送午餐通知失敗", e);
            return ResponseEntity.internalServerError()
                .body("Failed to send notification: " + e.getMessage());
        }
    }
}
