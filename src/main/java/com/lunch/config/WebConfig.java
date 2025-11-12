package com.lunch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 * 主要用於配置 CORS（跨域資源共享）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 允許的來源（開發階段允許所有，生產環境應指定具體域名）
                .allowedOrigins("*")
                // 允許的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允許的請求頭
                .allowedHeaders("*")
                // 是否允許攜帶認證信息（cookies）
                .allowCredentials(false)
                // 預檢請求的有效期（秒）
                .maxAge(3600);

        // 如果未來需要為 LIFF 添加特定配置，可以在這裡添加
        // 例如：
        // registry.addMapping("/liff/**")
        //         .allowedOrigins("https://liff.line.me")
        //         .allowedMethods("GET", "POST")
        //         .allowCredentials(true);
    }
}
