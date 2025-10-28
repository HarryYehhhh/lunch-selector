package com.lunch;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.FileInputStream;

/**
 * Firestore 連接測試
 */
public class FirestoreConnectionTest {

    public static void main(String[] args) {
        try {
            System.out.println("🔍 開始測試 Firestore 連接...\n");

            // 讀取環境變數
            String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            String projectId = System.getenv("GCP_PROJECT_ID");

            if (credentialsPath == null || credentialsPath.isEmpty()) {
                System.err.println("❌ 錯誤：未設定 GOOGLE_APPLICATION_CREDENTIALS 環境變數");
                System.err.println("請執行：export GOOGLE_APPLICATION_CREDENTIALS=\"$(pwd)/firestore-key.json\"");
                System.exit(1);
            }

            if (projectId == null || projectId.isEmpty()) {
                System.err.println("❌ 錯誤：未設定 GCP_PROJECT_ID 環境變數");
                System.err.println("請執行：export GCP_PROJECT_ID=\"your-project-id\"");
                System.exit(1);
            }

            System.out.println("📋 Project ID: " + projectId);
            System.out.println("🔑 Credentials: " + credentialsPath);
            System.out.println();

            // 初始化 Firestore
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream(credentialsPath)
            );

            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

            Firestore firestore = firestoreOptions.getService();

            System.out.println("✅ Firestore 連接成功！");
            System.out.println("📦 Database: " + firestore.getOptions().getDatabaseId());
            System.out.println();

            // 測試基本操作
            System.out.println("🧪 測試基本操作...");

            // 列出 collections
            Iterable<com.google.cloud.firestore.CollectionReference> collections =
                firestore.listCollections();

            System.out.println("📚 現有 Collections:");
            int count = 0;
            for (com.google.cloud.firestore.CollectionReference collection : collections) {
                System.out.println("  • " + collection.getId());
                count++;
            }

            if (count == 0) {
                System.out.println("  (目前還沒有 collections，這是正常的)");
            }

            System.out.println();
            System.out.println("🎉 所有測試通過！Firestore 設定正確！");

        } catch (Exception e) {
            System.err.println("\n❌ 連接失敗！");
            System.err.println("錯誤訊息：" + e.getMessage());
            System.err.println();
            System.err.println("可能的原因：");
            System.err.println("1. 金鑰文件路徑不正確");
            System.err.println("2. Project ID 不正確");
            System.err.println("3. 服務帳號沒有 Firestore 權限");
            System.err.println("4. Firestore 資料庫尚未創建");
            System.err.println();
            e.printStackTrace();
            System.exit(1);
        }
    }
}
