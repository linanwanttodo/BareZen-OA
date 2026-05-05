package com.linan.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.security.RequirePermission;
import com.linan.hr.domain.AttClockMonthlyStat;
import com.linan.hr.service.IAttClockRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AttClockController 单元测试
 *
 * <p>覆盖：权限注解；月份参数校验；统计参数（员工姓名/部门）透传与分页返回。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class AttClockControllerTest {

    @Mock
    private IAttClockRecordService clockService;

    private AttClockController controller;

    @BeforeEach
    void setUp() {
        controller = new AttClockController(clockService);
    }

    @Test
    @DisplayName("统计接口声明权限 hr:attendance:list")
    void page_hasPermissionAnnotation() throws Exception {
        Method page = AttClockController.class.getMethod("page", String.class, Integer.class,
                Integer.class, String.class, Long.class);
        RequirePermission perm = page.getAnnotation(RequirePermission.class);
        assertNotNull(perm, "必须声明 @RequirePermission");
        assertEquals("hr:attendance:list", perm.value()[0]);
    }

    @Test
    @DisplayName("非法月份返回格式错误")
    void page_invalidMonth_rejected() {
        var result = controller.page("2026/03", 1, 10, null, null);
        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("yyyy-MM"));
    }

    @Test
    @DisplayName("合法月份透传参数并返回分页结果")
    void page_passesParamsAndReturnsPage() {
        Page<AttClockMonthlyStat> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(new AttClockMonthlyStat()));
        page.setTotal(1);
        when(clockService.pageMonthlyStat(1, 10, YearMonth.of(2026, 3), "张", 5L)).thenReturn(page);

        var result = controller.page("2026-03", 1, 10, "张", 5L);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getTotal());
        ArgumentCaptor<YearMonth> month = ArgumentCaptor.forClass(YearMonth.class);
        ArgumentCaptor<String> userName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> deptId = ArgumentCaptor.forClass(Long.class);
        verify(clockService).pageMonthlyStat(anyLong(), anyLong(),
                month.capture(), userName.capture(), deptId.capture());
        assertEquals(YearMonth.of(2026, 3), month.getValue());
        assertEquals("张", userName.getValue());
        assertEquals(5L, deptId.getValue());
    }
}