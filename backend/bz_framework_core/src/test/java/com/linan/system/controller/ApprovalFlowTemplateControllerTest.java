package com.linan.system.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.ApprovalFlowTemplate;
import com.linan.system.service.IApprovalFlowTemplateService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * 审批流程模板 Controller 单元测试
 *
 * <p>覆盖：分页查询（按 bizType 过滤）、新增（同 bizType+nodeSeq 唯一校验、默认启用、
 * 非法 bizType/空节点名拒绝）、更新、删除。核心断言「提交按模板生成链」由
 * ApprovalFlowEngineServiceImplTest 覆盖（start 按模板首节点生成任务）。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class ApprovalFlowTemplateControllerTest {

    @Mock
    private IApprovalFlowTemplateService templateService;

    @InjectMocks
    private ApprovalFlowTemplateController controller;

    /**
     * 初始化 MyBatis-Plus 实体表信息缓存，使 LambdaQueryWrapper 在纯 Mockito 环境下可构建
     */
    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApprovalFlowTemplate.class);
    }

    private ApprovalFlowTemplate tpl(String bizType, Integer seq, String name) {
        ApprovalFlowTemplate t = new ApprovalFlowTemplate();
        t.setBizType(bizType);
        t.setNodeSeq(seq);
        t.setNodeName(name);
        t.setEnabled(1);
        return t;
    }

    @Test
    @DisplayName("分页查询：默认查询全部并按 bizType/nodeSeq 排序")
    void list_withoutFilter() {
        ApprovalFlowTemplate t = tpl("leave", 1, "请假审批");
        when(templateService.page(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<ApprovalFlowTemplate>(1, 10)
                        .setRecords(List.of(t)).setTotal(1));

        Result<PageResult<ApprovalFlowTemplate>> result = controller.list(1, 10, null);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().getTotal());
        assertEquals("leave", result.getData().getList().get(0).getBizType());
    }

    @Test
    @DisplayName("分页查询：按 bizType 过滤")
    void list_withBizTypeFilter() {
        when(templateService.page(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<ApprovalFlowTemplate>(1, 10).setRecords(List.of()).setTotal(0));

        controller.list(1, 10, "vehicle");

        ArgumentCaptor<LambdaQueryWrapper<ApprovalFlowTemplate>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(templateService).page(any(Page.class), captor.capture());
        String sql = String.valueOf(captor.getValue().getSqlSegment());
        assertTrue(sql.contains("biz_type"),
                "查询条件应包含 bizType 过滤，实际 SQL: " + sql);
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("vehicle"),
                "bizType 过滤值应为 vehicle");
    }

    @Test
    @DisplayName("新增：同 bizType+nodeSeq 重复被拒绝")
    void save_duplicateSeq_rejected() {
        ApprovalFlowTemplate t = tpl("leave", 1, "请假审批");
        when(templateService.count(any(Wrapper.class))).thenReturn(1L);

        Result<Void> result = controller.save(t);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("节点序号已存在"));
        verify(templateService, never()).save(any());
    }

    @Test
    @DisplayName("新增：合法节点保存成功且默认启用")
    void save_validPersisted() {
        ApprovalFlowTemplate t = tpl("leave", 2, "HR审批");
        t.setEnabled(null);
        when(templateService.count(any(Wrapper.class))).thenReturn(0L);
        when(templateService.save(any(ApprovalFlowTemplate.class))).thenReturn(true);

        Result<Void> result = controller.save(t);

        assertEquals(200, result.getCode());
        assertEquals(1, t.getEnabled(), "未显式传 enabled 时默认启用");
        verify(templateService).save(t);
    }

    @Test
    @DisplayName("新增：不支持的业务类型被拒绝")
    void save_unsupportedBizType_rejected() {
        ApprovalFlowTemplate t = tpl("supply", 1, "用品审批");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.save(t));

        assertTrue(ex.getMessage().contains("不支持的业务类型"));
        verify(templateService, never()).count(any());
        verify(templateService, never()).save(any());
    }

    @Test
    @DisplayName("新增：空节点名被拒绝")
    void save_blankNodeName_rejected() {
        ApprovalFlowTemplate t = tpl("leave", 3, "   ");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.save(t));

        assertTrue(ex.getMessage().contains("节点名称不能为空"));
        verify(templateService, never()).save(any());
    }

    @Test
    @DisplayName("新增：节点序号非正整数被拒绝")
    void save_invalidSeq_rejected() {
        ApprovalFlowTemplate t = tpl("leave", 0, "节点0");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.save(t));

        assertTrue(ex.getMessage().contains("节点序号"));
    }

    @Test
    @DisplayName("更新：合法节点保存成功")
    void update_valid() {
        ApprovalFlowTemplate t = tpl("leave", 2, "HR复核");
        when(templateService.updateById(any(ApprovalFlowTemplate.class))).thenReturn(true);

        Result<Void> result = controller.update(10L, t);

        assertEquals(200, result.getCode());
        assertEquals(10L, t.getId());
        verify(templateService).updateById(t);
    }

    @Test
    @DisplayName("删除：删除成功")
    void delete_valid() {
        when(templateService.removeById(10L)).thenReturn(true);

        Result<Void> result = controller.delete(10L);

        assertEquals(200, result.getCode());
        verify(templateService).removeById(10L);
    }

    @Test
    @DisplayName("删除：不存在的节点返回失败")
    void delete_missing_rejected() {
        when(templateService.removeById(99L)).thenReturn(false);

        Result<Void> result = controller.delete(99L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
    }
}