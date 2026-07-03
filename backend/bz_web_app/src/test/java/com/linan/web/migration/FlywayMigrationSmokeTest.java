package com.linan.web.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flyway 迁移真库冒烟测试。
 *
 * <p>此前 V51/V52 只在开发库上用「事务内执行后回滚」验过 SQL 语法，没有任何测试覆盖空库
 * 全量迁移与迁移所建立的数据不变量，于是下面这几类真实发生过的缺陷无人拦截：
 * <ul>
 *   <li>子菜单 path 混用两种约定 → vue-router 把相对 path 注册成顶层绝对路径，菜单点击 404，
 *       不同父级下的同名短 path 还互相遮蔽（V51 修）；</li>
 *   <li>建了 F 型按钮却漏授角色 → 管理员界面上成片的操作按钮不显示（V52 修）；</li>
 *   <li>菜单挂错父级（V40 把 id=17 当成资产管理，实为权限分配）。</li>
 * </ul>
 *
 * <p>做法：连接一个由本测试**自建自删**的临时库 {@code barezen_oa_migration_test}，跑完整
 * 迁移后校验不变量；绝不连业务库 {@code barezen_oa}。连接参数走环境变量（CI 用 mysql service
 * 容器即可），参数缺失或库不可达时按假设跳过，并把原因写进跳过消息——静默跳过会让零覆盖
 * 伪装成全绿。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Flyway 迁移冒烟测试")
class FlywayMigrationSmokeTest {

    private static final String SCRATCH_DB = "barezen_oa_migration_test";

    private static String adminUrl;
    private static String adminUser;
    private static String adminPassword;
    private static String scratchUrl;
    private static boolean dbReady;

    @BeforeAll
    static void prepareScratchDatabase() {
        adminUrl = System.getenv("TEST_DB_URL");
        adminUser = System.getenv("TEST_DB_USER");
        adminPassword = System.getenv("TEST_DB_PASSWORD");

        assumeTrue(adminUrl != null && !adminUrl.isBlank() && adminUser != null && adminPassword != null,
                "未设置 TEST_DB_URL / TEST_DB_USER / TEST_DB_PASSWORD，跳过真库迁移测试");
        assumeTrue(!adminUrl.contains("barezen_oa"),
                "TEST_DB_URL 指向业务库，拒绝执行（本测试会 DROP 目标库）");

        try (Connection conn = DriverManager.getConnection(adminUrl, adminUser, adminPassword);
             Statement st = conn.createStatement()) {
            st.executeUpdate("DROP DATABASE IF EXISTS `" + SCRATCH_DB + "`");
            st.executeUpdate("CREATE DATABASE `" + SCRATCH_DB + "` DEFAULT CHARACTER SET utf8mb4");
        } catch (SQLException e) {
            assumeTrue(false, "临时库准备失败，跳过：" + e.getMessage());
            return;
        }

        // 按 ? 切分重建，不用正则替换：正则易把查询串的分隔符一起吞掉，
        // 拼出畸形 URL 后报的是 DNS 解析失败，很难一眼看出是库名替换出的问题
        int q = adminUrl.indexOf('?');
        String base = q < 0 ? adminUrl : adminUrl.substring(0, q);
        String query = q < 0 ? "" : adminUrl.substring(q);
        scratchUrl = base.substring(0, base.lastIndexOf('/') + 1) + SCRATCH_DB + query;
        Flyway flyway = Flyway.configure()
                .dataSource(scratchUrl, adminUser, adminPassword)
                // 与 application.yml 的 flyway 配置保持一致：测的就是线上真正会跑的那套参数
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .table("flyway_schema_history")
                .outOfOrder(false)
                .load();
        var result = flyway.migrate();

        dbReady = result.success;
        assertTrue(result.success, "存在执行失败的迁移");
        try {
            assertEquals(migrationFileCount(), result.migrationsExecuted,
                    "空库全量迁移数应与 db/migration 下脚本数一致（说明有脚本未被执行或版本序列有洞）");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @AfterAll
    static void dropScratchDatabase() {
        if (adminUrl == null || adminUser == null || adminPassword == null) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(adminUrl, adminUser, adminPassword);
             Statement st = conn.createStatement()) {
            st.executeUpdate("DROP DATABASE IF EXISTS `" + SCRATCH_DB + "`");
        } catch (SQLException ignored) {
            // 清理失败不影响测试结论
        }
    }

    /** 以模块根目录为基准数脚本，断言随迁移新增自动跟进，不写死数量 */
    private static int migrationFileCount() throws Exception {
        try (var stream = Files.list(java.nio.file.Path.of("src/main/resources/db/migration"))) {
            return (int) stream.filter(p -> {
                String n = p.getFileName().toString();
                return n.startsWith("V") && n.endsWith(".sql");
            }).count();
        }
    }

    private List<String> firstColumn(String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(scratchUrl, adminUser, adminPassword);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(rs.getString(1));
            }
        }
        return rows;
    }

    private void requireDb() {
        assumeTrue(dbReady, "临时库未就绪，跳过");
    }

    @Test
    @DisplayName("无两个在用的 M/C 菜单共用同一 path（同名短 path 会互相遮蔽）")
    void menuPathsAreUniqueAmongLiveMenus() throws SQLException {
        requireDb();
        List<String> dup = firstColumn(
                "SELECT m.path FROM sys_menu m "
                        + "WHERE m.del_flag = 0 AND m.menu_type IN ('M','C') AND m.path IS NOT NULL AND m.path <> '' "
                        + "GROUP BY m.path HAVING COUNT(*) > 1");
        assertTrue(dup.isEmpty(), "存在重复菜单 path（后者被前者遮蔽并 404）：" + dup);
    }

    @Test
    @DisplayName("子菜单 path 必须含父前缀（相对约定会被 vue-router 注册到顶层）")
    void childMenuPathsAreParentPrefixed() throws SQLException {
        requireDb();
        List<String> bad = firstColumn(
                "SELECT CONCAT(m.id, ':', m.path, ' under ', p.path) FROM sys_menu m "
                        + "JOIN sys_menu p ON p.id = m.parent_id "
                        + "WHERE m.del_flag = 0 AND m.menu_type IN ('M','C') AND p.menu_type IN ('M','C') "
                        + "AND m.path IS NOT NULL AND m.path <> '' "
                        + "AND m.path <> p.path AND m.path NOT LIKE CONCAT(p.path, '/%')");
        assertTrue(bad.isEmpty(), "子菜单 path 未含父前缀，前端只能靠兜底拼接才能跳转：" + bad);
    }

    @Test
    @DisplayName("已授 C 菜单的角色必须同时持有其下 F 按钮（否则操作按钮不渲染）")
    void grantedMenuIncludesItsButtons() throws SQLException {
        requireDb();
        List<String> missing = firstColumn(
                "SELECT CONCAT(rm.role_id, '/', m.id, ':', m.perms) FROM sys_menu m "
                        + "JOIN sys_role_menu rm ON rm.menu_id = m.parent_id "
                        + "LEFT JOIN sys_role_menu rb ON rb.menu_id = m.id AND rb.role_id = rm.role_id "
                        + "WHERE m.del_flag = 0 AND m.menu_type = 'F' AND m.perms IS NOT NULL AND m.perms <> '' "
                        + "AND rb.menu_id IS NULL");
        assertTrue(missing.isEmpty(), "存在只授父菜单、未授按钮的授权缺口：" + missing);
    }

    @Test
    @DisplayName("迁移不得留下挂到已删/不存在父级的孤儿菜单")
    void noOrphanMenus() throws SQLException {
        requireDb();
        List<String> orphans = firstColumn(
                "SELECT CONCAT(m.id, '->', m.parent_id) FROM sys_menu m "
                        + "LEFT JOIN sys_menu p ON p.id = m.parent_id "
                        + "WHERE m.del_flag = 0 AND m.parent_id <> 0 AND (p.id IS NULL OR p.del_flag <> 0)");
        assertTrue(orphans.isEmpty(), "存在孤儿菜单（父级已删或不存在），其权限串在角色树上不可见：" + orphans);
    }

    @Test
    @DisplayName("种子管理员账号存在且状态正常")
    void seedAdminUsable() throws SQLException {
        requireDb();
        assertEquals(List.of("admin"),
                firstColumn("SELECT user_name FROM sys_user WHERE user_name = 'admin' AND del_flag = 0 AND status = '0'"),
                "迁移后应可直接以 admin 登录");
    }
}
