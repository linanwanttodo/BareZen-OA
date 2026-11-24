package com.linan.plugin.salary.controller;

import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.salary.entity.SalaryPaymentRecord;
import com.linan.plugin.salary.service.ISalaryPaymentRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工资发放Controller
 *
 * @author BareZen Team
 */
@Tag(name = "工资发放", description = "工资发放与统计接口")
@RestController
@RequestMapping("/api/salary/v1")
@RequiredArgsConstructor
public class SalaryPaymentController {

    private final ISalaryPaymentRecordService salaryPaymentRecordService;

    @Operation(summary = "分页查询发放记录")
    @RequirePermission("salary:payment:list")
    @GetMapping("/payments/page")
    public Result<PageResult<SalaryPaymentRecord>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "发放月份(YYYY-MM)") @RequestParam(required = false) String paymentMonth,
            @Parameter(description = "发放状态(0未发放 1已发放)") @RequestParam(required = false) String paymentStatus,
            @Parameter(description = "员工姓名模糊关键字") @RequestParam(required = false) String keyword) {
        return Result.page(salaryPaymentRecordService.pageRecords(pageNum, pageSize, paymentMonth, paymentStatus, keyword));
    }

    @Operation(summary = "查询发放记录详情")
    @RequirePermission("salary:payment:list")
    @GetMapping("/payments/{id}")
    public Result<SalaryPaymentRecord> getById(
            @Parameter(description = "发放记录ID", required = true) @PathVariable Long id) {
        return Result.success(salaryPaymentRecordService.getById(id));
    }

    @Operation(summary = "按月份生成发放草稿")
    @RequirePermission("salary:payment:execute")
    @PostMapping("/payments/generate")
    public Result<Integer> generate(@RequestBody GenerateRequest request) {
        if (request.getUserIds() != null && request.getUserIds().size() > 200) {
            throw new BusinessException("批量生成单次不超过 200 人");
        }
        int created = salaryPaymentRecordService.generateFromStructures(request.getPaymentMonth(), request.getUserIds());
        return Result.success(created);
    }

    @Operation(summary = "执行发放")
    @RequirePermission("salary:payment:execute")
    @PostMapping("/payments/execute")
    public Result<Integer> execute(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("发放记录列表不能为空");
        }
        if (ids.size() > 200) {
            throw new BusinessException("批量发放单次不超过 200 条");
        }
        return Result.success(salaryPaymentRecordService.executePayment(ids));
    }

    @Operation(summary = "撤销发放")
    @RequirePermission("salary:payment:execute")
    @PutMapping("/payments/{id}/revoke")
    public Result<Void> revoke(
            @Parameter(description = "发放记录ID", required = true) @PathVariable Long id) {
        salaryPaymentRecordService.revokePayment(id);
        return Result.success(null);
    }

    @Operation(summary = "修改手工发放记录")
    @RequirePermission("salary:payment:execute")
    @PutMapping("/payments")
    public Result<Void> update(@RequestBody SalaryPaymentRecord record) {
        // 已发放记录不允许再改动金额；金额字段由生成逻辑维护，这里仅放行备注等修正
        SalaryPaymentRecord existing = salaryPaymentRecordService.getById(record.getId());
        if (existing == null) {
            return Result.error("发放记录不存在");
        }
        if ("1".equals(existing.getPaymentStatus())) {
            return Result.error("已发放记录不可修改，请先撤销发放");
        }
        boolean ok = salaryPaymentRecordService.updateById(record);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    @Operation(summary = "删除发放记录")
    @RequirePermission("salary:payment:execute")
    @DeleteMapping("/payments/{id}")
    public Result<Void> delete(
            @Parameter(description = "发放记录ID", required = true) @PathVariable Long id) {
        SalaryPaymentRecord existing = salaryPaymentRecordService.getById(id);
        if (existing == null) {
            return Result.error("发放记录不存在");
        }
        if ("1".equals(existing.getPaymentStatus())) {
            return Result.error("已发放记录不可删除，请先撤销发放");
        }
        boolean ok = salaryPaymentRecordService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    @Operation(summary = "工资统计")
    @RequirePermission("salary:statistics:view")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(
            @Parameter(description = "发放月份(YYYY-MM)，空为全部") @RequestParam(required = false) String paymentMonth) {
        return Result.success(salaryPaymentRecordService.statistics(paymentMonth));
    }

    /**
     * 生成发放草稿请求体
     */
    @Data
    public static class GenerateRequest {
        @Parameter(description = "发放月份(YYYY-MM)")
        private String paymentMonth;
        @Parameter(description = "指定员工，空为全部有工资结构的员工")
        private List<Long> userIds;
    }
}
