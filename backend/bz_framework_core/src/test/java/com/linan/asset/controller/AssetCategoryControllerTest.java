package com.linan.asset.controller;

import com.linan.asset.dto.AssetCategoryDTO;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.service.IAssetCategoryService;
import com.linan.core.common.api.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

/**
 * 资产分类 Controller 单元测试
 *
 * <p>覆盖：分类列表、新增、更新、删除。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("资产分类Controller测试")
class AssetCategoryControllerTest {

    @Mock
    private IAssetCategoryService categoryService;

    @InjectMocks
    private AssetCategoryController controller;

    @Test
    @DisplayName("列表：返回全部分类")
    void list_returnsAll() {
        when(categoryService.listCategories(nullable(Integer.class)))
                .thenReturn(List.of(createCategory(1L, "电脑办公"), createCategory(2L, "设备")));

        Result<List<AssetCategory>> result = controller.list(null);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("新增：合法DTO保存成功")
    void save_validDTO_success() {
        AssetCategoryDTO dto = createDTO("会议室设备");
        
        assertDoesNotThrow(() -> controller.save(dto));
        
        verify(categoryService).addCategory(any(AssetCategory.class));
    }

    @Test
    @DisplayName("更新：合法DTO更新成功")
    void update_validDTO_success() {
        AssetCategoryDTO dto = createDTO("办公家具");
        
        assertDoesNotThrow(() -> controller.update(5L, dto));
        
        verify(categoryService).updateCategory(any(AssetCategory.class));
    }

    @Test
    @DisplayName("删除：删除成功")
    void delete_valid() {
        assertDoesNotThrow(() -> controller.delete(5L));
        
        verify(categoryService).deleteCategory(5L);
    }

    private AssetCategoryDTO createDTO(String name) {
        AssetCategoryDTO dto = new AssetCategoryDTO();
        dto.setName(name);
        return dto;
    }

    private AssetCategory createCategory(Long id, String name) {
        AssetCategory c = new AssetCategory();
        c.setId(id);
        c.setName(name);
        return c;
    }
}