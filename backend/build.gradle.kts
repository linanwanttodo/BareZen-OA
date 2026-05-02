plugins {
    id("java")
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.linan"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    // Java Toolchain 强制使用 JDK 21 编译
    // 即使系统默认 java 是其他版本，Gradle 也会自动查找 JDK 21
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        // 引入 Spring Boot BOM 统一管理版本
        implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))

        // 公共依赖（所有子模块都需要）
        compileOnly("org.projectlombok:lombok:1.18.34")
        annotationProcessor("org.projectlombok:lombok:1.18.34")
        testImplementation(platform("org.junit:junit-bom:6.0.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        // Jsoup HTML 净化（XSS 防护）
        implementation("org.jsoup:jsoup:1.18.1")

        // Swagger 注解（@Tag, @Operation 等）
        implementation("io.swagger.core.v3:swagger-annotations:2.2.21")
    }

    tasks.test {
        useJUnitPlatform()
    }

    // 编译时保留参数名信息（Spring MVC 需要）
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }
}