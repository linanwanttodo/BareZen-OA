# ============================================
# BareZen_Base 后端 Docker 多阶段构建
# ============================================
# 构建阶段
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 先复制构建配置文件，利用 Docker 缓存加速
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle gradle
RUN chmod +x gradlew

# 下载依赖（仅当 build.gradle.kts 变化时才会重新执行）
RUN ./gradlew dependencies --no-daemon --quiet 2>/dev/null || true

# 复制源码并构建
COPY bz_framework_core bz_framework_core
COPY bz_web_app bz_web_app
RUN ./gradlew :bz_web_app:bootJar --no-daemon

# ============================================
# 运行阶段 - 最小化镜像
FROM eclipse-temurin:21-jre-alpine

# 安全：使用非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 只复制构建产物（Fat Jar），不需要 libs/
COPY --from=builder /app/bz_web_app/build/libs/*.jar app.jar

# 健康检查
RUN apk add --no-cache curl
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -sf http://localhost:8080/api/common/v1/health || exit 1

EXPOSE 8080

ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=prod

# 使用非 root 用户
USER appuser

# JVM 优化：启用虚拟线程、使用 G1GC、设置内存限制
ENTRYPOINT ["java", \
    "-jar", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+ExitOnOutOfMemoryError", \
    "/app/app.jar"]
