plugins {
    id("java")
    // 应用 Spring Boot 插件，提供 bootRun、bootJar 等任务
    id("org.springframework.boot")
}

group = "com.linan"
version = "1.0-SNAPSHOT"

dependencies {
    // 引入核心底座模块（提供 Result、JWT、异常处理等基础能力）
    implementation(project(":bz_framework_core"))

    // Spring Boot Web 启动器（嵌入式 Tomcat + Spring MVC + Jackson）
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot AOP（@Aspect 注解支持）
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Boot Data Redis（在线用户监控、接口限流、防重复提交需要）
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Lettuce 连接池支持（spring.data.redis.lettuce.pool.* 配置依赖 commons-pool2，
    // 缺少此依赖时连接池配置会被静默忽略，导致并发下连接数不受控）
    implementation("org.apache.commons:commons-pool2")

    // Spring Boot Cache（权限缓存、字典缓存）
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Flyway 数据库迁移
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // Spring Boot WebSocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Quartz 定时任务
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // JWT（WebSocket 认证需要）：版本必须与 bz_framework_core 一致，
    // 否则 Gradle 冲突仲裁向下取版本，core 声明的 CVE-2024-31033 修复会失效
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // MyBatis-Plus（LambdaQueryWrapper、Page 等在 web 模块中直接使用）
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.7")

    // Commons IO（文件上传工具类）
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Knife4j 接口文档
    implementation("com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:4.5.0")

    // 测试依赖（由根构建脚本统一管理版本）
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    // Gradle 守护进程不会继承本次命令行的环境变量，故同时支持 -P 属性转发给测试 JVM，
    // 保证 TEST_DB_* 在守护进程复用下也能抵达
    listOf("TEST_DB_URL", "TEST_DB_USER", "TEST_DB_PASSWORD").forEach { key ->
        (project.findProperty(key) as? String)?.let { environment(key, it) }
    }

    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
