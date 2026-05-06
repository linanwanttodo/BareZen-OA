plugins {
    id("java")
}

group = "com.linan"
version = "1.0-SNAPSHOT"

// 仓库配置已在根项目的 settings.gradle.kts 中统一管理，无需重复配置

dependencies {
    // Spring Boot Web 基础依赖
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring AOP 支持（@Aspect、@Around 等注解）
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Redis 缓存支持
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Lombok 简化代码（版本由根构建脚本的 BOM 管理）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // JWT 支持 (jjwt)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Spring Security Crypto (BCryptPasswordEncoder 密码加密)
    implementation("org.springframework.security:spring-security-crypto")

    // Bean Validation API（@Valid、@NotBlank 等参数校验注解）
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Commons IO（文件上传工具类）
    implementation("commons-io:commons-io:2.15.1")

    // MyBatis-Plus 持久层框架（版本由根构建脚本的 BOM 管理）
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.7")

    // AWS SDK S3（对象存储抽象实现，S3 协议兼容 MinIO / 阿里云 OSS / 腾讯 COS）
    implementation("software.amazon.awssdk:s3:2.25.61")
    // 轻量 HTTP 客户端（避免引入 Netty，减小依赖体积）
    implementation("software.amazon.awssdk:url-connection-client:2.25.61")

    // MySQL 数据库驱动
    runtimeOnly("com.mysql:mysql-connector-j")

    // Quartz 定时任务调度框架
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}