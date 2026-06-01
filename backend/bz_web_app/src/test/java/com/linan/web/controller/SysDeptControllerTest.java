package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.system.entity.SysDept;
import com.linan.system.service.ISysDeptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDeptController 部门管理 CRUD 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysDeptControllerTest {

    @Mock
    private ISysDeptService deptService;

    private SysDeptController controller;

    @BeforeEach
    void setUp() {
        controller = new SysDeptController(deptService);
    }

    @Test
    @DisplayName("tree 返回部门树形列表")
    void tree_returnsDeptTree() {
        List<SysDept> tree = List.of(new SysDept());
        when(deptService.getDeptTree()).thenReturn(tree);

        Result<List<SysDept>> result = controller.tree();

        assertEquals(200, result.getCode());
        assertEquals(tree, result.getData());
        verify(deptService).getDeptTree();
    }

    @Test
    @DisplayName("get 按 ID 查询部门详情")
    void get_returnsDeptById() {
        SysDept dept = new SysDept();
        dept.setId(1L);
        when(deptService.getById(1L)).thenReturn(dept);

        Result<SysDept> result = controller.get(1L);

        assertEquals(200, result.getCode());
        assertEquals(1L, result.getData().getId());
        verify(deptService).getById(1L);
    }

    @Test
    @DisplayName("save 新增部门成功")
    void save_success() {
        SysDept dept = new SysDept();
        when(deptService.save(any(SysDept.class))).thenReturn(true);

        Result<Void> result = controller.save(dept);

        assertEquals(200, result.getCode());
        verify(deptService).save(dept);
    }

    @Test
    @DisplayName("save 新增部门失败")
    void save_failed() {
        when(deptService.save(any(SysDept.class))).thenReturn(false);

        Result<Void> result = controller.save(new SysDept());

        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("update 更新部门成功")
    void update_success() {
        SysDept dept = new SysDept();
        when(deptService.updateById(any(SysDept.class))).thenReturn(true);

        Result<Void> result = controller.update(dept);

        assertEquals(200, result.getCode());
        verify(deptService).updateById(dept);
    }

    @Test
    @DisplayName("delete 有子部门拒绝删除")
    void delete_hasChild_rejected() {
        when(deptService.hasChildDept(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(500, result.getCode());
        assertEquals("存在子部门，无法删除", result.getMsg());
    }

    @Test
    @DisplayName("delete 无子部门删除成功")
    void delete_noChild_success() {
        when(deptService.hasChildDept(1L)).thenReturn(false);
        when(deptService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(deptService).removeById(1L);
    }

    @Test
    @DisplayName("changeStatus 切换部门状态")
    void changeStatus_success() {
        Result<Void> result = controller.changeStatus(1L, "1");

        assertEquals(200, result.getCode());
        verify(deptService).updateDeptStatus(1L, "1");
    }
}