package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageParamGuard;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.plugin.salary.entity.SalaryPaymentRecord;
import com.linan.plugin.salary.entity.SalaryStructure;
import com.linan.plugin.salary.service.ISalaryPaymentRecordService;
import com.linan.plugin.salary.service.ISalaryStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的工资条 Controller（用户前台 /api/app/v1/salary）
 *
 * <p>员工自助查看本人工资发放记录与当前工资结构。工资数据敏感：
 * 所有查询强制以当前登录用户 userId 为过滤条件，接口不接收任何 userId 参数，
 * 详情额外做归属校验，防止越权读取他人薪资。</p>
 *
 * <p>发放记录由管理端按月生成并执行，不涉及审批流，无需操作 AppFlowLogController。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "我的工资条", description = "用户前台 - 我的工资条")
@RestController
@RequestMapping("/api/app/v1/salary")
@RequiredArgsConstructor
public class AppSalaryController {

    private final ISalaryPaymentRecordService payrollService;

    private final ISalaryStructureService structureService;

    /**
     * 我的工资条列表（按发放月份倒序）
     * GET /api/app/v1/salary/payrolls
     */
    @Operation(summary = "我的工资条列表")
    @GetMapping("/payrolls")
    public Result<PageResult<SalaryPaymentRecord>> myPayrolls(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        // 归属条件由服务端强制注入，绝不信任前端传参
        LambdaQueryWrapper<SalaryPaymentRecord> wrapper = new LambdaQueryWrapper<SalaryPaymentRecord>()
                .eq(SalaryPaymentRecord::getUserId, userId)
                .orderByDesc(SalaryPaymentRecord::getPaymentMonth)
                .orderByDesc(SalaryPaymentRecord::getId);
        IPage<SalaryPaymentRecord> result = payrollService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 工资条详情
     * GET /api/app/v1/salary/payrolls/{id}
     */
    @Operation(summary = "工资条详情")
    @GetMapping("/payrolls/{id}")
    public Result<SalaryPaymentRecord> payrollDetail(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        SalaryPaymentRecord record = payrollService.getById(id);
        if (record == null) {
            return Result.error("工资条不存在");
        }
        // 归属校验：仅允许查看本人的工资条
        if (!userId.equals(record.getUserId())) {
            return Result.error("无权查看该工资条");
        }
        return Result.success(record);
    }

    /**
     * 我的当前工资结构
     * GET /api/app/v1/salary/structure
     */
    @Operation(summary = "我的当前工资结构")
    @GetMapping("/structure")
    public Result<SalaryStructure> myStructure() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        // 复用管理端 GET /api/salary/v1/structures/user/{userId} 同一查询方法
        return Result.success(structureService.getByUserId(userId));
    }
}
