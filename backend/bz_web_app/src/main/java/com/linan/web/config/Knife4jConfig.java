package com.linan.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Knife4j/Swagger OpenAPI 3.0 文档配置
 *
 * <p>仅在开发环境（dev）下加载，生产环境（prod）不会暴露 API 文档。
 * 通过 {@code @Profile("dev")} 确保生产环境无法访问 /doc.html、/v3/api-docs 等端点。</p>
 *
 * <p>规范要点（OpenAPI 3.0）：</p>
 * <ul>
 *   <li>Info：标题/描述/版本/联系方式/许可</li>
 *   <li>Server：服务地址（可通过配置注入）</li>
 *   <li>SecurityScheme：Bearer JWT 认证，全局声明并应用于全部接口</li>
 *   <li>Tag：预置业务域分组说明</li>
 * </ul>
 */
@Configuration
@Profile("dev")
public class Knife4jConfig {

    /** 文档服务地址，可通过 knife4j.server-url 覆盖（默认 http://localhost:8080） */
    @Value("${knife4j.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // ===== Info：接口文档元信息 =====
                .info(new Info()
                        .title("BareZen_Base API 文档")
                        .description("BareZen_Base 前后端分离公司内部管理系统接口文档。\n\n"
                                + "**统一约定**：\n"
                                + "- 请求/响应均为 JSON（application/json）\n"
                                + "- 成功响应统一结构：`{ code: 200, msg: \"操作成功\", data: ... }`\n"
                                + "- 业务错误：`{ code: 500, msg: \"错误说明\", data: null }`\n"
                                + "- 鉴权失败：`{ code: 401, msg: \"未登录或Token已过期\" }`\n"
                                + "- 权限不足：`{ code: 403, msg: \"没有访问权限\" }`")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BareZen Team")
                                .email("admin@barezen.com"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://barezen.example.com")))
                // ===== Server：服务地址（可通过配置注入）=====
                .servers(List.of(new Server()
                        .url(serverUrl)
                        .description("BareZen_Base 服务")))
                // ===== SecurityScheme：Bearer JWT 认证 =====
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录后获取的 JWT Token，格式：Bearer <token>")))
                // ===== 全局要求：所有接口默认携带 Bearer Token =====
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                // ===== Tag：业务域分组说明 =====
                .tags(List.of(
                        new Tag().name("认证").description("登录/登出/验证码/注册"),
                        new Tag().name("用户管理").description("系统管理 - 用户 CRUD 与角色分配"),
                        new Tag().name("角色管理").description("系统管理 - 角色与权限"),
                        new Tag().name("菜单管理").description("系统管理 - 菜单与动态路由"),
                        new Tag().name("部门管理").description("系统管理 - 组织架构"),
                        new Tag().name("字典管理").description("系统管理 - 数据字典"),
                        new Tag().name("系统配置").description("系统管理 - 参数配置"),
                        new Tag().name("通知公告").description("系统管理 - 公告发布/回执"),
                        new Tag().name("文件管理").description("系统管理 - 文件上传下载"),
                        new Tag().name("登录日志").description("系统监控 - 登录日志"),
                        new Tag().name("操作日志").description("系统监控 - 操作日志"),
                        new Tag().name("在线用户").description("系统监控 - 在线会话"),
                        new Tag().name("定时任务").description("系统监控 - 定时任务管理"),
                        new Tag().name("任务日志").description("系统监控 - 任务执行日志"),
                        new Tag().name("数据备份").description("系统监控 - 数据库备份"),
                        new Tag().name("仪表盘").description("管理端首页数据聚合"),
                        new Tag().name("统一待办中心").description("聚合各业务模块待审批事项"),
                        new Tag().name("员工档案").description("人事管理 - 员工档案"),
                        new Tag().name("请假审批").description("考勤管理 - 请假申请与审批"),
                        new Tag().name("加班申请").description("考勤管理 - 加班申请与审批"),
                        new Tag().name("出差申请").description("考勤管理 - 出差申请与审批"),
                        new Tag().name("工作日志").description("人事管理 - 日报/周报与批阅"),
                        new Tag().name("资产台账").description("资产管理 - 固定资产台账"),
                        new Tag().name("会议室管理").description("行政办公 - 会议室管理"),
                        new Tag().name("会议预订").description("行政办公 - 会议预订"),
                        new Tag().name("用车申请").description("行政办公 - 用车申请与审批"),
                        new Tag().name("办公用品").description("行政办公 - 用品管理与申领"),
                        new Tag().name("知识文档").description("知识库 - 文档沉淀"),
                        new Tag().name("审批流程进度").description("申请单流程节点查询"),
                        new Tag().name("员工门户").description("用户前台 - 工作台/公告/通讯录/待办"),
                        new Tag().name("员工自助申请").description("用户前台 - 加班/出差自助申请"),
                        new Tag().name("个人日程").description("用户前台 - 日程管理"),
                        new Tag().name("办公用品申领").description("用户前台 - 用品申领"),
                        new Tag().name("公共接口").description("健康检查等公共端点")));
    }
}
