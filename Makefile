# ============================================
# Makefile - 簡化 Docker 操作
# ============================================

.PHONY: help build build-no-cache run stop clean logs test deploy

# 默認目標
.DEFAULT_GOAL := help

# 變數
IMAGE_NAME := lunch-selector
CONTAINER_NAME := lunch-selector
PORT := 8081

## help: 顯示幫助信息
help:
	@echo "可用的命令："
	@echo "  make build           - 構建 Docker image（使用緩存）"
	@echo "  make build-no-cache  - 構建 Docker image（不使用緩存，生產推薦）"
	@echo "  make run             - 啟動容器"
	@echo "  make stop            - 停止並刪除容器"
	@echo "  make restart         - 重啟容器"
	@echo "  make logs            - 查看容器日誌"
	@echo "  make logs-f          - 實時查看容器日誌"
	@echo "  make test            - 測試構建"
	@echo "  make clean           - 清理 Docker 資源"
	@echo "  make clean-all       - 清理所有 Docker 資源（包括緩存）"

## build: 使用緩存構建（開發用）
build:
	@echo "🔨 使用緩存構建..."
	USE_CACHE=yes ./build.sh

## build-no-cache: 不使用緩存構建（生產推薦）
build-no-cache:
	@echo "🔨 完全重新構建（不使用緩存）..."
	USE_CACHE=no ./build.sh

## test: 構建並測試
test:
	@echo "🧪 構建並測試..."
	USE_CACHE=no RUN_TEST=yes ./build.sh

## run: 啟動容器
run: stop
	@echo "🚀 啟動容器..."
	@if [ ! -f .env.local ]; then \
		echo "❌ .env.local 文件不存在！"; \
		exit 1; \
	fi
	@bash -c 'source .env.local && docker run -d \
		--name $(CONTAINER_NAME) \
		-p $(PORT):8080 \
		-v "$${PWD}/firestore-key.json:/app/firestore-key.json:ro" \
		-e GOOGLE_APPLICATION_CREDENTIALS="/app/firestore-key.json" \
		-e GCP_PROJECT_ID="$${GCP_PROJECT_ID}" \
		-e LINE_CHANNEL_TOKEN="$${LINE_CHANNEL_TOKEN}" \
		-e LINE_CHANNEL_SECRET="$${LINE_CHANNEL_SECRET}" \
		$(IMAGE_NAME):latest'
	@echo "✅ 容器已啟動"
	@sleep 3
	@make logs

## stop: 停止並刪除容器
stop:
	@echo "🛑 停止容器..."
	@docker stop $(CONTAINER_NAME) 2>/dev/null || true
	@docker rm $(CONTAINER_NAME) 2>/dev/null || true
	@echo "✅ 容器已停止"

## restart: 重啟容器
restart: stop run

## logs: 查看日誌
logs:
	@docker logs $(CONTAINER_NAME) 2>&1 | tail -30

## logs-f: 實時查看日誌
logs-f:
	@docker logs -f $(CONTAINER_NAME)

## clean: 清理未使用的 Docker 資源
clean:
	@echo "🧹 清理未使用的資源..."
	docker image prune -f
	docker container prune -f

## clean-all: 清理所有 Docker 資源（包括緩存）
clean-all:
	@echo "🧹 清理所有 Docker 資源（包括構建緩存）..."
	docker builder prune -af
	docker image prune -af
	docker container prune -f
	@echo "✅ 清理完成"

## deploy: 構建並部署（生產流程）
deploy: build-no-cache run
	@echo "🎉 部署完成！"
	@echo "容器狀態："
	@docker ps | grep $(CONTAINER_NAME) || echo "❌ 容器未運行"
