package com.linan.plugin.salary.entity;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 薪酬实体 MyBatis-Plus SQL 生成契约测试
 *
 * <p>removeById 的行为分支点在 {@link TableInfo#isWithLogicDelete()}：为 true 生成
 * 逻辑删除 UPDATE（del_flag=1），为 false 生成真 DELETE。本测试不依赖数据库，
 * 直接用 TableInfoHelper 构建两表元数据，锁定「删除后可重建」在框架层的成立条件：</p>
 * <ul>
 *   <li>salary_structure 物理删除：DELETE 后 uk_user_id (user_id) 立即释放，可重建；</li>
 *   <li>salary_payment_record 逻辑删除：唯一键含 del_flag（V56），删除后同月可重新生成。</li>
 * </ul>
 */
class SalaryDeleteSqlContractTest {

    private TableInfo initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfigUtils.setGlobalConfig(configuration, GlobalConfigUtils.defaults());
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        return TableInfoHelper.initTableInfo(assistant, entityClass);
    }

    @Test
    @DisplayName("工资结构：removeById 生成物理 DELETE（isWithLogicDelete=false）")
    void salaryStructure_generatesPhysicalDelete() {
        TableInfo info = initTableInfo(SalaryStructure.class);
        assertFalse(info.isWithLogicDelete(),
                "salary_structure 必须物理删除：逻辑删除会让 user_id 永久占用 uk_user_id，删除后无法重建");
    }

    @Test
    @DisplayName("工资发放记录：removeById 生成逻辑删除 UPDATE（isWithLogicDelete=true）")
    void salaryPaymentRecord_generatesLogicDelete() {
        TableInfo info = initTableInfo(SalaryPaymentRecord.class);
        assertTrue(info.isWithLogicDelete(),
                "salary_payment_record 必须逻辑删除，发放记录不可物理删除");
        assertEquals("del_flag", info.getLogicDeleteFieldInfo().getColumn(),
                "逻辑删除字段应为 del_flag 列");
    }
}
