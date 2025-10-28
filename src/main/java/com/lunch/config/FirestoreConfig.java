package com.lunch.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firestore 配置
 */
@Slf4j
@Configuration
public class FirestoreConfig {

    @Value("${gcp.project-id:}")
    private String projectId;

    @Value("${gcp.credentials-path:}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        FirestoreOptions.Builder optionsBuilder = FirestoreOptions.newBuilder();

        // 設定 Project ID
        if (projectId != null && !projectId.isEmpty()) {
            optionsBuilder.setProjectId(projectId);
            log.info("使用指定的 GCP Project ID: {}", projectId);
        }

        // 設定認證
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            // 使用指定的服務帳號金鑰文件
            try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                optionsBuilder.setCredentials(credentials);
                log.info("使用服務帳號金鑰文件進行認證: {}", credentialsPath);
            }
        } else {
            // 在 GCP 環境中（Cloud Run, Compute Engine 等），會自動使用預設認證
            log.info("使用應用程式預設認證（Application Default Credentials）");
        }

        Firestore firestore = optionsBuilder.build().getService();
        log.info("✅ Firestore 初始化成功");

        return firestore;
    }
}
