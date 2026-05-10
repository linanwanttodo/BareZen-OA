package com.linan.core.common.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集成测试基类
 *
 * 提供测试环境的基础配置和通用方法。
 * Spring Boot Test 的 {@code @Transactional} 在每个测试方法结束后自动回滚，
 * 无需手动管理事务提交/回滚。
 *
 * @author BareZen Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    /**
     * 在每个测试方法前执行的初始化逻辑。
     * 子类可覆盖此方法添加额外的 setup。
     */
    @BeforeEach
    void setupTransaction() {
        // @Transactional 已在每个测试方法结束后自动回滚，无需手动处理
    }
}