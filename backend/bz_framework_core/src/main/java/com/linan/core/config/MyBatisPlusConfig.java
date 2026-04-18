package com.linan.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置类
 *
 * 负责：
 * 1. 注册分页插件（PaginationInnerInterceptor），使分页查询生效
 * 2. 指定 Mapper 接口的扫描路径，避免在每个 Mapper 上单独添加 @Mapper 注解
 *
 * 注意：MapperScan 注解的包路径 "com.linan.**.mapper" 使用了通配符 **，
 * 可匹配多级子目录，因此所有子模块的 mapper 包都会被自动扫描。
 * 例如：com.linan.system.mapper、com.linan.common.mapper 等。
 */
@Configuration
@MapperScan("com.linan.**.mapper")  // 扫描所有子模块中的 Mapper 接口
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 核心拦截器
     *
     * MybatisPlusInterceptor 是 MyBatis-Plus 3.4+ 版本的统一拦截器容器。
     * 通过向其中添加不同的 InnerInterceptor（内部拦截器），可以启用：
     * - 分页插件（PaginationInnerInterceptor）
     * - 乐观锁插件（OptimisticLockerInnerInterceptor）
     * - 防止全表更新/删除插件（BlockAttackInnerInterceptor）
     * - 动态表名插件（DynamicTableNameInnerInterceptor）
     *
     * 当前仅启用分页插件，后续需要其他功能时在此方法中添加即可。
     *
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建统一的拦截器容器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 创建分页内部拦截器，DbType.MYSQL 指定生成 MySQL 兼容的分页 SQL
        // 分页 SQL 示例：SELECT ... FROM table LIMIT offset, size
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);

        // 设置单页最大查询条数，防止 pageSize=99999 拖垮数据库
        paginationInterceptor.setMaxLimit(1000L);

        // 溢出处理：请求页码超过总页数时，自动设为最后一页
        // 例如总页数为 5，请求第 10 页时返回第 5 页数据，而非空列表
        paginationInterceptor.setOverflow(true);

        // 将分页拦截器注册到容器中
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}