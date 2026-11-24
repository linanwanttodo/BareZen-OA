package com.linan.plugin.salary.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 薪酬实体删除语义契约测试
 *
 * <p>两表唯一键不含 del_flag（V47 uk_user_id、V56 后的 uk_user_month 均以业务维度为准，
 * 与 V49「业务唯一性只看业务维度」的项目取向一致），删除语义按表分工：</p>
 * <ul>
 *   <li>{@link SalaryStructure}：配置数据无审计价值，必须物理删除。
 *       曾因漏标 @TableLogic 意外真删而补标，V56 起反转为契约：若误标 @TableLogic，
 *       removeById 退化为软删，user_id 永久占用 uk_user_id，员工工资结构无法重建；</li>
 *   <li>{@link SalaryPaymentRecord}：发放记录有审计价值，必须逻辑删除。唯一键已含
 *       del_flag（V56），删除后同月可重新生成；二次软删同月仍会冲突（见 V56 注释）。</li>
 * </ul>
 */
class SalaryLogicDeleteContractTest {

    private Field delFlagField(Class<?> entityClass) throws NoSuchFieldException {
        return entityClass.getDeclaredField("delFlag");
    }

    @Test
    @DisplayName("工资结构实体禁用逻辑删除（物理删除，删除后 user_id 可立即重建）")
    void salaryStructure_forbidsTableLogic() throws NoSuchFieldException {
        Field field = delFlagField(SalaryStructure.class);
        assertNull(field.getAnnotation(TableLogic.class), SalaryStructure.class.getSimpleName()
                + ".delFlag 不得标注 @TableLogic：软删会永久占用 uk_user_id (user_id)，员工无法重建工资结构");
    }

    @Test
    @DisplayName("工资发放记录实体启用逻辑删除")
    void salaryPaymentRecord_declaresTableLogic() throws NoSuchFieldException {
        Field field = delFlagField(SalaryPaymentRecord.class);
        assertNotNull(field.getAnnotation(TableLogic.class), SalaryPaymentRecord.class.getSimpleName()
                + ".delFlag 必须标注 @TableLogic，发放记录不可物理删除");
    }

    @Test
    @DisplayName("删除标记不得出现在对外响应中")
    void delFlag_isHiddenFromApiPayload() throws NoSuchFieldException {
        for (Class<?> entityClass : new Class<?>[]{SalaryStructure.class, SalaryPaymentRecord.class}) {
            Field field = delFlagField(entityClass);
            assertNotNull(field.getAnnotation(JsonIgnore.class),
                    entityClass.getSimpleName() + ".delFlag 应标注 @JsonIgnore，避免删除标记泄漏到接口响应");
        }
    }
}
