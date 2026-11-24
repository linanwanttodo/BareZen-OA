package com.linan.plugin.masterdata;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.masterdata.controller.SupplierInfoController;
import com.linan.plugin.masterdata.entity.SupplierInfo;
import com.linan.plugin.masterdata.service.ISupplierInfoService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 供应商 Controller 单元测试
 *
 * <p>覆盖 @RequirePermission 权限串与 V58 菜单 F 行对齐、CRUD 委托与
 * 编码冲突异常路径。权限拦截本身由 RequirePermissionAspect 集成链路执行。</p>
 */
@ExtendWith(MockitoExtension.class)
class SupplierInfoControllerTest {

    @Mock
    private ISupplierInfoService supplierInfoService;

    @InjectMocks
    private SupplierInfoController controller;

    @Test
    @DisplayName("五个接口权限串与 V58 菜单 F 行一致（masterdata:supplier:*）")
    void permissions_alignedWithMenu() throws Exception {
        assertEquals("masterdata:supplier:list", SupplierInfoController.class
                .getMethod("page", long.class, long.class, String.class, String.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:supplier:list", SupplierInfoController.class
                .getMethod("getById", Long.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:supplier:add", SupplierInfoController.class
                .getMethod("save", SupplierInfo.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:supplier:edit", SupplierInfoController.class
                .getMethod("update", SupplierInfo.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:supplier:delete", SupplierInfoController.class
                .getMethod("delete", Long.class)
                .getAnnotation(RequirePermission.class).value()[0]);
    }

    @Test
    @DisplayName("分页：委托 service.page 并返回分页数据")
    void page_delegates() {
        Page<SupplierInfo> page = new Page<>(1, 10);
        SupplierInfo supplier = new SupplierInfo();
        supplier.setId(1L);
        supplier.setSupplierCode("GYS001");
        page.setRecords(List.of(supplier));
        page.setTotal(1);
        when(supplierInfoService.page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any())).thenReturn(page);

        Result<com.linan.core.common.api.PageResult<SupplierInfo>> result = controller.page(1, 10, "GYS", null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        verify(supplierInfoService).page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("详情：委托 getById")
    void getById_delegates() {
        SupplierInfo supplier = new SupplierInfo();
        supplier.setId(1L);
        when(supplierInfoService.getById(1L)).thenReturn(supplier);

        Result<?> result = controller.getById(1L);

        assertEquals(200, result.getCode());
        assertEquals(supplier, result.getData());
    }

    @Test
    @DisplayName("新增：委托 createSupplier 返回保存结果")
    void save_delegates() {
        SupplierInfo supplier = new SupplierInfo();
        supplier.setSupplierCode("GYS002");
        supplier.setSupplierName("供应商二");
        when(supplierInfoService.createSupplier(supplier)).thenReturn(supplier);

        Result<?> result = controller.save(supplier);

        assertEquals(200, result.getCode());
        assertEquals(supplier, result.getData());
    }

    @Test
    @DisplayName("新增：编码重复 service 抛 BusinessException 原样穿透")
    void save_duplicateCode_throws() {
        SupplierInfo supplier = new SupplierInfo();
        when(supplierInfoService.createSupplier(supplier))
                .thenThrow(new BusinessException("供应商编码已存在: GYS002"));

        BusinessException e = assertThrows(BusinessException.class, () -> controller.save(supplier));
        assertTrue(e.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("更新：委托 updateSupplier")
    void update_delegates() {
        SupplierInfo supplier = new SupplierInfo();
        supplier.setId(1L);
        when(supplierInfoService.updateSupplier(supplier)).thenReturn(supplier);

        Result<?> result = controller.update(supplier);

        assertEquals(200, result.getCode());
        verify(supplierInfoService).updateSupplier(supplier);
    }

    @Test
    @DisplayName("删除：removeById 成功返回 200")
    void delete_success() {
        when(supplierInfoService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(supplierInfoService).removeById(1L);
    }

    @Test
    @DisplayName("删除：removeById 失败返回错误")
    void delete_failed() {
        when(supplierInfoService.removeById(99L)).thenReturn(false);

        Result<Void> result = controller.delete(99L);

        assertEquals(500, result.getCode());
        verify(supplierInfoService).removeById(99L);
    }

    @Test
    @DisplayName("分页不触发写操作")
    void page_isReadOnly() {
        Page<SupplierInfo> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(supplierInfoService.page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any())).thenReturn(page);

        controller.page(1, 10, null, null);

        verify(supplierInfoService, never()).save(org.mockito.ArgumentMatchers.any(SupplierInfo.class));
        verify(supplierInfoService, never()).removeById(org.mockito.ArgumentMatchers.anyLong());
    }
}
