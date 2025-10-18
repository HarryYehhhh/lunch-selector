#!/bin/bash

# 本地執行腳本
set -e

echo "🍱 啟動午餐選擇器..."
echo ""

# 檢查 .env
if [ ! -f .env ]; then
    echo "⚠️  找不到 .env 檔案"
    read -p "是否要建立 .env 檔案? (Y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        cp .env.example .env
        echo "✅ 已建立 .env 檔案"
        echo "📝 請編輯 .env 填入你的 LINE 設定，然後重新執行此腳本"
        exit 0
    fi
fi

# 載入環境變數
echo "🔑 載入環境變數..."
export $(cat .env | grep -v '^#' | xargs)

# 檢查 Java 和 Maven
command -v java >/dev/null 2>&1 || { echo "❌ 需要安裝 Java"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ 需要安裝 Maven"; exit 1; }

# 執行應用程式
echo "🚀 啟動 Spring Boot 應用程式..."
echo ""
mvn spring-boot:run
