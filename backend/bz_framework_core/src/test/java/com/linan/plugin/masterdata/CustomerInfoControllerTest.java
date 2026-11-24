package com.linan.plugin.masterdata;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.masterdata.controller.CustomerInfoController;
import com.linan.plugin.masterdata.entity.CustomerInfo;
import com.linan.plugin.masterdata.service.ICustomerInfoService;
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
 * 客户 Controller 单元测试
 *
 * <p>覆盖 @RequirePermission 权限串与 V58 菜单 F 行对齐、CRUD 委托与
 * 编码冲突异常路径。</p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerInfoControllerTest {

    @Mock
    private ICustomerInfoService customerInfoService;

    @InjectMocks
    private CustomerInfoController controller;

    @Test
    @DisplayName("五个接口权限串与 V58 菜单 F 行一致（masterdata:customer:*）")
    void permissions_alignedWithMenu() throws Exception {
        assertEquals("masterdata:customer:list", CustomerInfoController.class
                .getMethod("page", long.class, long.class, String.class, String.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:customer:list", CustomerInfoController.class
                .getMethod("getById", Long.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:customer:add", CustomerInfoController.class
                .getMethod("save", CustomerInfo.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:customer:edit", CustomerInfoController.class
                .getMethod("update", CustomerInfo.class)
                .getAnnotation(RequirePermission.class).value()[0]);
        assertEquals("masterdata:customer:delete", CustomerInfoController.class
                .getMethod("delete", Long.class)
                .getAnnotation(RequirePermission.class).value()[0]);
    }

    @Test
    @DisplayName("分页：委托 service.page 并返回分页数据")
    void page_delegates() {
        Page<CustomerInfo> page = new Page<>(1, 10);
        CustomerInfo customer = new CustomerInfo();
        customer.setId(1L);
        customer.setCustomerCode("KH001");
        page.setRecords(List.of(customer));
        page.setTotal(1);
        when(customerInfoService.page(any(Page.class), any())).thenReturn(page);

        Result<com.linan.core.common.api.PageResult<CustomerInfo>> result = controller.page(1, 10, "KH", "0");

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        verify(customerInfoService).page(any(Page.class), any());
    }

    @Test
    @DisplayName("详情：委托 getById")
    void getById_delegates() {
        CustomerInfo customer = new CustomerInfo();
        customer.setId(1L);
        when(customerInfoService.getById(1L)).thenReturn(customer);

        Result<?> result = controller.getById(1L);

        assertEquals(200, result.getCode());
        assertEquals(customer, result.getData());
    }

    @Test
    @DisplayName("新增：委托 createCustomer 返回保存结果")
    void save_delegates() {
        CustomerInfo customer = new CustomerInfo();
        customer.setCustomerCode("KH002");
        customer.setCustomerName("客户二");
        when(customerInfoService.createCustomer(customer)).thenReturn(customer);

        Result<?> result = controller.save(customer);

        assertEquals(200, result.getCode());
        assertEquals(customer, result.getData());
    }

    @Test
    @DisplayName("新增：编码重复 service 抛 BusinessException 原样穿透")
    void save_duplicateCode_throws() {
        CustomerInfo customer = new CustomerInfo();
        when(customerInfoService.createCustomer(customer))
                .thenThrow(new BusinessException("客户编码已存在: KH002"));

        BusinessException e = assertThrows(BusinessException.class, () -> controller.save(customer));
        assertTrue(e.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("更新：委托 updateCustomer")
    void update_delegates() {
        CustomerInfo customer = new CustomerInfo();
        customer.setId(1L);
        when(customerInfoService.updateCustomer(customer)).thenReturn(customer);

        Result<?> result = controller.update(customer);

        assertEquals(200, result.getCode());
        verify(customerInfoService).updateCustomer(customer);
    }

    @Test
    @DisplayName("删除：removeById 成功返回 200，失败返回错误")
    void delete_paths() {
        when(customerInfoService.removeById(1L)).thenReturn(true);
        assertEquals(200, controller.delete(1L).getCode());

        when(customerInfoService.removeById(99L)).thenReturn(false);
        assertEquals(500, controller.delete(99L).getCode());

        verify(customerInfoService).removeById(1L);
        verify(customerInfoService).removeById(99L);
    }
}
