rootProject.name = "BareZen_Base"

// 包含子模块
include("bz_framework_core")
include("bz_web_app")

pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // gradlePluginPortal()  // 如果阿里云没有的插件，可以取消注释这行
        // mavenCentral()         // 作为最后的备选，通常不需要
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // mavenCentral()  // 阿里云已包含 Maven Central 的内容，通常不需要重复配置
    }
}
