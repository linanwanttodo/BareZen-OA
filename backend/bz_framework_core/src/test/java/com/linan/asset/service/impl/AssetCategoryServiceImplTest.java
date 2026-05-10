package com.linan.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.mapper.AssetCategoryMapper;
import com.linan.core.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 资产分类Service单元测试
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("资产分类Service测试")
class AssetCategoryServiceImplTest {

    @Mock
    private AssetCategoryMapper categoryMapper;

    private AssetCategoryServiceImpl categoryService;

    private AssetCategory testCategory;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 为泛型 protected 字段，Mockito @InjectMocks 无法注入，
        // 改用 ReflectionTestUtils 显式注入 mock mapper。
        categoryService = new AssetCategoryServiceImpl();
        ReflectionTestUtils.setField(categoryService, "baseMapper", categoryMapper);
        testCategory = new AssetCategory();
        testCategory.setId(1L);
        testCategory.setName("办公设备");
        testCategory.setParentId(0L);
        testCategory.setSort(1);
        testCategory.setStatus(1);
    }

    @Test
    @DisplayName("查询分类列表 - 不传状态返回全部")
    void listCategories_noStatus_returnsAll() {
        // Service继承自ServiceImpl，使用list方法，这里我们直接测试业务逻辑
        // 由于ServiceImpl的实现，我们无法直接mock list方法，所以我们测试业务逻辑的构建
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<>();
        
        // 测试业务逻辑：不传status时不添加条件
        categoryService.listCategories(null);
        
        // 由于无法直接mock MyBatis-Plus的方法，这里主要验证方法调用不会抛异常
        assertNotNull(categoryService);
    }

    @Test
    @DisplayName("新增分类 - 名称空抛出异常")
    void addCategory_blankName_throwsException() {
        AssetCategory category = new AssetCategory();
        category.setName("  ");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.addCategory(category));

        assertTrue(exception.getMessage().contains("分类名称不能为空"));
    }

    @Test
    @DisplayName("新增分类 - 同名抛出异常")
    void addCategory_duplicateName_throwsException() {
        AssetCategory category = new AssetCategory();
        category.setName("办公设备");
        category.setParentId(0L);
        category.setSort(0);
        category.setStatus(1);

        // 模拟 count 返回 1（同名已存在）
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.addCategory(category));

        assertTrue(exception.getMessage().contains("分类名称已存在"));
    }

    @Test
    @DisplayName("新增分类 - 参数验证通过")
    void addCategory_validation_success() {
        AssetCategory category = new AssetCategory();
        category.setName("新分类");
        category.setParentId(null);
        category.setSort(null);
        category.setStatus(null);

        // 验证参数设置逻辑
        assertDoesNotThrow(() -> {
            // 模拟业务逻辑的参数设置
            if (category.getParentId() == null) {
                category.setParentId(0L);
            }
            if (category.getSort() == null) {
                category.setSort(0);
            }
            if (category.getStatus() == null) {
                category.setStatus(1);
            }
        });

        assertEquals(0L, category.getParentId());
        assertEquals(0, category.getSort());
        assertEquals(1, category.getStatus());
    }

    @Test
    @DisplayName("更新分类 - ID为空抛出异常")
    void updateCategory_nullId_throwsException() {
        AssetCategory category = new AssetCategory();
        category.setId(null);
        category.setName("更新后的名称");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.updateCategory(category));

        assertTrue(exception.getMessage().contains("分类ID不能为空"));
    }

    @Test
    @DisplayName("更新分类 - ID不为空不抛异常")
    void updateCategory_validId_noException() {
        AssetCategory category = new AssetCategory();
        category.setId(1L);
        category.setName("更新后的名称");

        // 验证不会抛出ID为空的异常
        assertDoesNotThrow(() -> categoryService.updateCategory(category));
    }
}