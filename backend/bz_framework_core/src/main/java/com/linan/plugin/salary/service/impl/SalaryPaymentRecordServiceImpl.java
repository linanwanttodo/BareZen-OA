package com.linan.plugin.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.salary.entity.SalaryPaymentRecord;
import com.linan.plugin.salary.entity.SalaryStructure;
import com.linan.plugin.salary.mapper.SalaryPaymentRecordMapper;
import com.linan.plugin.salary.service.ISalaryPaymentRecordService;
import com.linan.plugin.salary.service.ISalaryStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 工资发放记录服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class SalaryPaymentRecordServiceImpl extends ServiceImpl<SalaryPaymentRecordMapper, SalaryPaymentRecord>
        implements ISalaryPaymentRecordService {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ISalaryStructureService salaryStructureService;

    @Override
    public IPage<SalaryPaymentRecord> pageRecords(long pageNum, long pageSize,
                                                  String paymentMonth, String paymentStatus, String keyword) {
        LambdaQueryWrapper<SalaryPaymentRecord> wrapper = new LambdaQueryWrapper<SalaryPaymentRecord>()
                .eq(paymentMonth != null && !paymentMonth.isBlank(), SalaryPaymentRecord::getPaymentMonth, paymentMonth)
                .eq(paymentStatus != null && !paymentStatus.isBlank(), SalaryPaymentRecord::getPaymentStatus, paymentStatus)
                .like(keyword != null && !keyword.isBlank(), SalaryPaymentRecord::getUserName, keyword)
                .orderByDesc(SalaryPaymentRecord::getPaymentMonth)
                .orderByAsc(SalaryPaymentRecord::getId);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateFromStructures(String paymentMonth, List<Long> userIds) {
        validateMonth(paymentMonth);

        List<SalaryStructure> structures = salaryStructureService.list(
                new LambdaQueryWrapper<SalaryStructure>()
                        .in(userIds != null && !userIds.isEmpty(), SalaryStructure::getUserId, userIds));
        if (structures.isEmpty()) {
            throw new BusinessException("没有可生成发放记录的工资结构，请先维护员工工资结构");
        }

        // 幂等：跳过该月份已存在的员工记录（含逻辑删除后唯一键仍占用的极端情况由插入冲突兜底）
        Set<Long> existingUserIds = list(new LambdaQueryWrapper<SalaryPaymentRecord>()
                .eq(SalaryPaymentRecord::getPaymentMonth, paymentMonth)
                .in(SalaryPaymentRecord::getUserId, structures.stream().map(SalaryStructure::getUserId).collect(Collectors.toList()))
                .select(SalaryPaymentRecord::getUserId))
                .stream().map(SalaryPaymentRecord::getUserId).collect(Collectors.toCollection(HashSet::new));

        List<SalaryPaymentRecord> records = new ArrayList<>();
        for (SalaryStructure structure : structures) {
            if (existingUserIds.contains(structure.getUserId())) {
                continue;
            }
            records.add(buildDraft(structure, paymentMonth));
        }
        if (records.isEmpty()) {
            throw new BusinessException(paymentMonth + " 的发放记录已全部生成，请勿重复操作");
        }
        saveBatch(records);
        return records.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int executePayment(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要发放的工资记录");
        }
        LocalDateTime now = LocalDateTime.now();
        // 仅未发放记录可执行发放，条件更新天然幂等
        return baseMapper.update(null, new LambdaUpdateWrapper<SalaryPaymentRecord>()
                .in(SalaryPaymentRecord::getId, ids)
                .eq(SalaryPaymentRecord::getPaymentStatus, "0")
                .set(SalaryPaymentRecord::getPaymentStatus, "1")
                .set(SalaryPaymentRecord::getPaymentTime, now)
                .set(SalaryPaymentRecord::getUpdateTime, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePayment(Long id) {
        SalaryPaymentRecord record = getById(id);
        if (record == null) {
            throw new BusinessException("发放记录不存在");
        }
        if (!"1".equals(record.getPaymentStatus())) {
            throw new BusinessException("该记录尚未发放，无需撤销");
        }
        // set(field, null) 显式清空发放时间（MP 默认忽略 null 字段）
        update(new LambdaUpdateWrapper<SalaryPaymentRecord>()
                .eq(SalaryPaymentRecord::getId, id)
                .set(SalaryPaymentRecord::getPaymentStatus, "0")
                .set(SalaryPaymentRecord::getPaymentTime, null)
                .set(SalaryPaymentRecord::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public Map<String, Object> statistics(String paymentMonth) {
        List<SalaryPaymentRecord> records = list(new LambdaQueryWrapper<SalaryPaymentRecord>()
                .eq(paymentMonth != null && !paymentMonth.isBlank(), SalaryPaymentRecord::getPaymentMonth, paymentMonth));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summarize(records));

        // 按月分布（全量数据分组，供报表画趋势；指定月份时仅该月一组）
        Map<String, List<SalaryPaymentRecord>> byMonth = records.stream()
                .collect(Collectors.groupingBy(SalaryPaymentRecord::getPaymentMonth, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> monthly = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("paymentMonth", e.getKey());
                    item.putAll(summarize(e.getValue()));
                    return item;
                })
                .collect(Collectors.toList());
        result.put("monthly", monthly);
        return result;
    }

    private Map<String, Object> summarize(List<SalaryPaymentRecord> records) {
        Map<String, Object> summary = new LinkedHashMap<>();
        BigDecimal totalIncome = ZERO, totalDeduction = ZERO, totalActual = ZERO;
        int paidCount = 0;
        for (SalaryPaymentRecord r : records) {
            totalIncome = totalIncome.add(nvl(r.getTotalIncome()));
            totalDeduction = totalDeduction.add(nvl(r.getTotalDeduction()));
            totalActual = totalActual.add(nvl(r.getActualSalary()));
            if ("1".equals(r.getPaymentStatus())) {
                paidCount++;
            }
        }
        summary.put("headcount", records.size());
        summary.put("paidCount", paidCount);
        summary.put("unpaidCount", records.size() - paidCount);
        summary.put("totalIncome", totalIncome);
        summary.put("totalDeduction", totalDeduction);
        summary.put("totalActual", totalActual);
        return summary;
    }

    /**
     * 由工资结构快照生成当月发放草稿，金额口径与 SalaryStructureServiceImpl.calculateActualSalary 一致
     */
    private SalaryPaymentRecord buildDraft(SalaryStructure s, String paymentMonth) {
        SalaryPaymentRecord r = new SalaryPaymentRecord();
        r.setUserId(s.getUserId());
        r.setUserName(s.getUserName());
        r.setPaymentMonth(paymentMonth);
        r.setBaseSalary(nvl(s.getBaseSalary()));
        r.setPerformanceSalary(nvl(s.getPerformanceSalary()));
        r.setPositionAllowance(nvl(s.getPositionAllowance()));
        r.setTransportAllowance(nvl(s.getTransportAllowance()));
        r.setMealAllowance(nvl(s.getMealAllowance()));
        r.setOtherAllowance(nvl(s.getOtherAllowance()));
        r.setSocialInsurancePersonal(nvl(s.getSocialInsurancePersonal()));
        r.setHousingFundPersonal(nvl(s.getHousingFundPersonal()));
        r.setIncomeTax(nvl(s.getIncomeTax()));
        r.setTotalIncome(r.getBaseSalary()
                .add(r.getPerformanceSalary())
                .add(r.getPositionAllowance())
                .add(r.getTransportAllowance())
                .add(r.getMealAllowance())
                .add(r.getOtherAllowance()));
        r.setTotalDeduction(r.getSocialInsurancePersonal()
                .add(r.getHousingFundPersonal())
                .add(r.getIncomeTax()));
        r.setActualSalary(r.getTotalIncome().subtract(r.getTotalDeduction()));
        r.setPaymentStatus("0");
        r.setRemark("由 " + paymentMonth + " 工资结构自动生成");
        return r;
    }

    private void validateMonth(String paymentMonth) {
        if (paymentMonth == null || !MONTH_PATTERN.matcher(paymentMonth).matches()) {
            throw new BusinessException("发放月份格式不正确，应为 YYYY-MM");
        }
        try {
            java.time.YearMonth.parse(paymentMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new BusinessException("发放月份不是有效日期：" + paymentMonth);
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? ZERO : v;
    }
}
