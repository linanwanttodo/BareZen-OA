package com.linan.plugin.masterdata;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.masterdata.controller.MaterialInfoController;
import com.linan.plugin.masterdata.entity.MaterialInfo;
import com.linan.plugin.masterdata.service.IMaterialInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 物料 Controller 单元测试
 *
 * <p>覆盖 @RequirePermission 权限串与 V58 菜单 F 行对齐、CRUD 委托与
 * 编码冲突异常路径。</p>
 */
@ExtendWith(MockitoExtension.class)
class MaterialInfoControllerTest {

    @Mock
    private IMaterialInfoService materialInfoService;

    @InjectMocks
    private MaterialInfoController controller;

    @Test
    @DisplayName("五个接口权限串与 V58 菜单 F 行一致（masterdata:material:*）")
    void permissions_alignedWithMenu() throws Exception {
        assertEquals("masterdata:material:list", MaterialInfoController.class
                .getMethod("page", long.class, long.class, String.class, String.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:material:list", MaterialInfoController.class
                .getMethod("getById", Long.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:material:add", MaterialInfoController.class
                .getMethod("save", MaterialInfo.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:material:edit", MaterialInfoController.class
                .getMethod("update", MaterialInfo.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:material:delete", MaterialInfoController.class
                .getMethod("delete", Long.class)
                .getAnnotation(RequirePermission.class).value()[0]);
    }

    @Test
    @DisplayName("分页：委托 service.page 并返回分页数据")
    void page_delegates() {
        Page<MaterialInfo> page = new Page<>(1, 10);
        MaterialInfo material = new MaterialInfo();
        material.setId(1L);
        material.setMaterialCode("WL001");
        page.setRecords(List.of(material));
        page.setTotal(1);
        when(materialInfoService.page(any(Page.class), any())).thenReturn(page);

        Result<com.linan.core.common.api.PageResult<MaterialInfo>> result = controller.page(1, 10, "WL", null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        verify(materialInfoService).page(any(Page.class), any());
    }

    @Test
    @DisplayName("详情：委托 getById")
    void getById_delegates() {
        MaterialInfo material = new MaterialInfo();
        material.setId(1L);
        when(materialInfoService.getById(1L)).thenReturn(material);

        Result<?> result = controller.getById(1L);

        assertEquals(200, result.getCode());
        assertEquals(material, result.getData());
    }

    @Test
    @DisplayName("新增：委托 createMaterial 返回保存结果")
    void save_delegates() {
        MaterialInfo material = new MaterialInfo();
        material.setMaterialCode("WL002");
        material.setMaterialName("物料二");
        when(materialInfoService.createMaterial(material)).thenReturn(material);

        Result<?> result = controller.save(material);

        assertEquals(200, result.getCode());
        assertEquals(material, result.getData());
    }

    @Test
    @DisplayName("新增：编码重复 service 抛 BusinessException 原样穿透")
    void save_duplicateCode_throws() {
        MaterialInfo material = new MaterialInfo();
        when(materialInfoService.createMaterial(material))
                .thenThrow(new BusinessException("物料编码已存在: WL002"));

        BusinessException e = assertThrows(BusinessException.class, () -> controller.save(material));
        assertTrue(e.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("更新：委托 updateMaterial")
    void update_delegates() {
        MaterialInfo material = new MaterialInfo();
        material.setId(1L);
        when(materialInfoService.updateMaterial(material)).thenReturn(material);

        Result<?> result = controller.update(material);

        assertEquals(200, result.getCode());
        verify(materialInfoService).updateMaterial(material);
    }

    @Test
    @DisplayName("删除：removeById 成功返回 200，失败返回错误")
    void delete_paths() {
        when(materialInfoService.removeById(1L)).thenReturn(true);
        assertEquals(200, controller.delete(1L).getCode());

        when(materialInfoService.removeById(99L)).thenReturn(false);
        assertEquals(500, controller.delete(99L).getCode());

        verify(materialInfoService).removeById(1L);
        verify(materialInfoService).removeById(99L);
    }
}
