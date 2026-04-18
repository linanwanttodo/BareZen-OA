.PHONY: env-up env-down dev-api clean

# 启动数据库和 Redis 环境
env-up:
	docker-compose up -d

# 停止并移除环境
env-down:
	docker-compose down

# 启动后端开发服务 (bz_web_app)
dev-api:
	./gradlew :bz_web_app:bootRun

# 清理构建产物
clean:
	./gradlew clean
