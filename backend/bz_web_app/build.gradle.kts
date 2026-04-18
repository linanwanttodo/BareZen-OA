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