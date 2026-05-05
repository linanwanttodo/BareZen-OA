package com.linan.kb.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.kb.entity.KbCategory;
import com.linan.kb.service.IKbCategoryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识库分类 Controller 单元测试（2.3）
 *
 * <p>覆盖：分类列表、新增（名称必填/同名唯一/默认值）、更新、删除。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class KbCategoryControllerTest {

    @Mock
    private IKbCategoryService categoryService;

    @InjectMocks
    private KbCategoryController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), KbCategory.class);
    }

    private KbCategory cat(Long id, String name) {
        KbCategory c = new KbCategory();
        c.setId(id);
        c.setName(name);
        return c;
    }

    @Test
    @DisplayName("列表：返回全部分类")
    void list_returnsAll() {
        when(categoryService.list(any(Wrapper.class))).thenReturn(List.of(cat(1L, "通用"), cat(2L, "制度手册")));

        Result<List<KbCategory>> result = controller.list(null);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("新增：名称空被拒绝")
    void save_blankName_rejected() {
        KbCategory c = new KbCategory();
        c.setName("  ");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.save(c));

        assertTrue(ex.getMessage().contains("分类名称"));
        verify(categoryService, never()).save(any());
    }

    @Test
    @DisplayName("新增：同名分类被拒绝")
    void save_duplicateName_rejected() {
        KbCategory c = cat(null, "通用");
        when(categoryService.count(any(Wrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.save(c));

        assertTrue(ex.getMessage().contains("已存在"));
        verify(categoryService, never()).save(any());
    }

    @Test
    @DisplayName("新增：合法分类保存成功且默认值生效")
    void save_validPersisted() {
        KbCategory c = new KbCategory();
        c.setName("运维手册");
        when(categoryService.count(any(Wrapper.class))).thenReturn(0L);
        when(categoryService.save(any(KbCategory.class))).thenReturn(true);

        Result<Void> result = controller.save(c);

        assertEquals(200, result.getCode());
        assertEquals(0L, c.getParentId(), "未传父分类时默认根分类");
        assertEquals(1, c.getStatus(), "未传状态时默认启用");
        assertNotNull(c.getSort());
    }

    @Test
    @DisplayName("更新：合法更新成功且 id 生效")
    void update_valid() {
        KbCategory c = cat(5L, "技术资料");
        when(categoryService.updateById(any(KbCategory.class))).thenReturn(true);

        Result<Void> result = controller.update(5L, c);

        assertEquals(200, result.getCode());
        assertEquals(5L, c.getId());
    }

    @Test
    @DisplayName("删除：删除成功")
    void delete_valid() {
        when(categoryService.removeById(5L)).thenReturn(true);

        Result<Void> result = controller.delete(5L);

        assertEquals(200, result.getCode());
        verify(categoryService).removeById(5L);
    }
}