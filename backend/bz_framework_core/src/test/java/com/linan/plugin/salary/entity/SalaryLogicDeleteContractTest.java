package com.linan.plugin.salary.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 薪酬实体逻辑删除契约测试
 *
 * <p>{@code salary_structure} 表带有 {@code del_flag} 列，但实体曾遗漏
 * {@code @TableLogic}，使 {@code removeById} 退化为真实 DELETE，薪酬记录不可恢复；
 * 同域的 {@link SalaryPaymentRecord} 则一直带该注解。本测试锁定两个薪酬实体
 * 都必须声明逻辑删除，防止后续新增实体再次漏标。</p>
 */
class SalaryLogicDeleteContractTest {

    private void assertLogicDeleteAnnotated(Class<?> entityClass) {
        Field field;
        try {
            field = entityClass.getDeclaredField("delFlag");
        } catch (NoSuchFieldException e) {
            throw new AssertionError(entityClass.getSimpleName() + " 必须保留 delFlag 字段", e);
        }
        TableLogic tableLogic = field.getAnnotation(TableLogic.class);
        assertNotNull(tableLogic, entityClass.getSimpleName()
                + ".delFlag 必须标注 @TableLogic，否则 removeById 会物理删除薪酬数据");
    }

    @Test
    @DisplayName("工资结构实体启用逻辑删除")
    void salaryStructure_declaresTableLogic() {
        assertLogicDeleteAnnotated(SalaryStructure.class);
    }

    @Test
    @DisplayName("工资发放记录实体启用逻辑删除")
    void salaryPaymentRecord_declaresTableLogic() {
        assertLogicDeleteAnnotated(SalaryPaymentRecord.class);
    }

    @Test
    @DisplayName("删除标记不得出现在对外响应中")
    void delFlag_isHiddenFromApiPayload() throws NoSuchFieldException {
        for (Class<?> entityClass : new Class<?>[]{SalaryStructure.class, SalaryPaymentRecord.class}) {
            Field field = entityClass.getDeclaredField("delFlag");
            assertNotNull(field.getAnnotation(JsonIgnore.class),
                    entityClass.getSimpleName() + ".delFlag 应标注 @JsonIgnore，避免删除标记泄漏到接口响应");
        }
    }
}
