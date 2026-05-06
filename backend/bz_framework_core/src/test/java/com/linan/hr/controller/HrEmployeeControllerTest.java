package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrEmployee;
import com.linan.hr.service.IHrEmployeeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 员工档案 Controller 单元测试
 *
 * <p>覆盖：列表身份证脱敏（隐私保护）、详情保留完整值、条件过滤转发、CRUD 成功/失败分支。</p>
 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeControllerTest {

    @Mock
    private IHrEmployeeService employeeService;

    @InjectMocks
    private HrEmployeeController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), HrEmployee.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    private HrEmployee employee(String idCard) {
        HrEmployee e = new HrEmployee();
        e.setName("张三");
        e.setEmpNo("E001");
        e.setIdCard(idCard);
        return e;
    }

    private Page<HrEmployee> pageOf(HrEmployee... records) {
        Page<HrEmployee> page = new Page<>(1, 10);
        page.setRecords(List.of(records));
        page.setTotal(records.length);
        return page;
    }

    @Test
    @DisplayName("列表：身份证号脱敏（保留前4后4）")
    void list_idCardMasked() {
        HrEmployee emp = employee("110101199001011234");
        when(employeeService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageOf(emp));

        Result<?> result = controller.list(1, 10, null, null, null, null);

        @SuppressWarnings("unchecked")
        List<HrEmployee> records = (List<HrEmployee>) ((com.linan.core.common.api.PageResult<?>) result.getData()).getList();
        assertEquals("1101**********1234", records.get(0).getIdCard());
    }

    @Test
    @DisplayName("列表：8 位及以下身份证号统一脱敏为 ****")
    void list_shortIdCardMasked() {
        HrEmployee emp = employee("12345678");
        when(employeeService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageOf(emp));

        Result<?> result = controller.list(1, 10, null, null, null, null);

        @SuppressWarnings("unchecked")
        List<HrEmployee> records = (List<HrEmployee>) ((com.linan.core.common.api.PageResult<?>) result.getData()).getList();
        assertEquals("****", records.get(0).getIdCard());
    }

    @Test
    @DisplayName("列表：姓名条件透传为 like 条件")
    void list_nameFilterApplied() {
        when(employeeService.page(any(Page.class), any(Wrapper.class))).thenReturn(pageOf());

        controller.list(1, 10, "张", null, null, null);

        verify(employeeService).page(any(Page.class),
                argThat(w -> w.getTargetSql().contains("name")));
    }

    @Test
    @DisplayName("详情：身份证号保留完整值供编辑回显")
    void getById_idCardKeptFull() {
        HrEmployee emp = employee("110101199001011234");
        emp.setId(9L);
        when(employeeService.getById(9L)).thenReturn(emp);

        Result<HrEmployee> result = controller.getById(9L);

        assertEquals(200, result.getCode());
        assertEquals("110101199001011234", result.getData().getIdCard(), "详情接口不应脱敏");
    }

    @Test
    @DisplayName("详情：员工不存在返回错误")
    void getById_missing_rejected() {
        when(employeeService.getById(99L)).thenReturn(null);

        Result<HrEmployee> result = controller.getById(99L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
    }

    @Test
    @DisplayName("新增：保存成功")
    void save_success() {
        when(employeeService.save(any(HrEmployee.class))).thenReturn(true);

        Result<Void> result = controller.save(employee("110101199001011234"));

        assertEquals(200, result.getCode());
        verify(employeeService).save(any(HrEmployee.class));
    }

    @Test
    @DisplayName("修改：保存失败返回错误")
    void update_failure() {
        HrEmployee emp = employee("110101199001011234");
        when(employeeService.updateById(any(HrEmployee.class))).thenReturn(false);

        Result<Void> result = controller.update(emp);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("失败"));
    }

    @Test
    @DisplayName("删除：成功且 id 转发")
    void delete_success() {
        when(employeeService.removeById(7L)).thenReturn(true);

        Result<Void> result = controller.delete(7L);

        assertEquals(200, result.getCode());
        verify(employeeService).removeById(7L);
    }

    @Test
    @DisplayName("删除：不存在返回错误")
    void delete_missing_rejected() {
        when(employeeService.removeById(7L)).thenReturn(false);

        Result<Void> result = controller.delete(7L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("失败"));
    }
}