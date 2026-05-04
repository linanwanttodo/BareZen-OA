package com.linan.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.linan.core.security.DataScopeInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置类
 *
 * 负责：
 * 1. 注册分页插件（PaginationInnerInterceptor），使分页查询生效
 * 2. 注册数据权限过滤（DataScopeInterceptor），自动追加数据范围条件
 * 3. 指定 Mapper 接口的扫描路径，避免在每个 Mapper 上单独添加 @Mapper 注解
 */
@Configuration
@MapperScan("com.linan.**.mapper")  // 扫描所有子模块中的 Mapper 接口
public class MyBatisPlusConfig {

    private final DataScopeInterceptor dataScopeInterceptor;

    /**
     * 构造器注入 + {@code @Lazy}：延迟加载 DataScopeInterceptor，
     * 打破 Mapper → sqlSessionFactory → MyBatisPlusConfig → DataScopeInterceptor 的循环依赖。
     */
    public MyBatisPlusConfig(@Lazy DataScopeInterceptor dataScopeInterceptor) {
        this.dataScopeInterceptor = dataScopeInterceptor;
    }

    /**
     * MyBatis-Plus 核心拦截器
     *
     * MybatisPlusInterceptor 是 MyBatis-Plus 3.4+ 版本的统一拦截器容器。
     * 通过向其中添加不同的 InnerInterceptor（内部拦截器），可以启用：
     * - 数据权限过滤（DataScopeInterceptor）
     * - 分页插件（PaginationInnerInterceptor）
     * - 乐观锁插件（OptimisticLockerInnerInterceptor）
     * - 防止全表更新/删除插件（BlockAttackInnerInterceptor）
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 数据权限过滤（必须在分页之前执行，确保 WHERE 条件先被注入）
        interceptor.addInnerInterceptor(dataScopeInterceptor);

        // 分页内部拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L);
        paginationInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
