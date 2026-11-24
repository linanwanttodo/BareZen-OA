package com.linan.plugin.masterdata.service.impl;

import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.masterdata.entity.DocNumberRule;
import com.linan.plugin.masterdata.mapper.DocNumberRuleMapper;
import com.linan.plugin.masterdata.service.IDocNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 单据号服务实现
 *
 * <p>发号流程：事务内 FOR UPDATE 锁定规则行 -> 计算日期段（跨日则序号从 0 起）
 * -> 乐观护栏更新序号（以读到的 current_seq/last_date 二元组为前置条件）->
 * 拼接前缀+日期段+4位序号。行锁使同一 bizKey 的并发发号串行化；乐观护栏在
 * 行锁失效的场景下仍能拒绝重复发号，两层保障并发不重号。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocNumberServiceImpl implements IDocNumberService {

    private static final DateTimeFormatter FALLBACK_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DocNumberRuleMapper ruleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String nextNumber(String bizKey) {
        if (bizKey == null || bizKey.isBlank()) {
            throw new BusinessException("单据号业务键不能为空");
        }
        DocNumberRule rule = ruleMapper.selectByBizKeyForUpdate(bizKey);
        if (rule == null) {
            throw new BusinessException("单据号规则不存在: " + bizKey);
        }
        if (rule.getPrefix() == null || rule.getPrefix().isBlank()) {
            throw new BusinessException("单据号规则前缀未配置: " + bizKey);
        }

        String dateStr = formatDate(rule);
        long readSeq = rule.getCurrentSeq() != null ? rule.getCurrentSeq() : 0L;
        String readDate = rule.getLastDate();
        long step = rule.getStep() != null && rule.getStep() > 0 ? rule.getStep() : 1L;
        long nextSeq = (dateStr.equals(readDate) ? readSeq : 0L) + step;

        int updated = ruleMapper.updateSeq(rule.getId(), readSeq, readDate, nextSeq, dateStr);
        if (updated != 1) {
            log.warn("单据号并发冲突: bizKey={}, readSeq={}, readDate={}", bizKey, readSeq, readDate);
            throw new BusinessException("单据号并发冲突，请重试: " + bizKey);
        }
        return rule.getPrefix() + dateStr + "-" + String.format("%04d", nextSeq);
    }

    private String formatDate(DocNumberRule rule) {
        try {
            String pattern = rule.getDateFormat() == null || rule.getDateFormat().isBlank()
                    ? "yyyyMMdd" : rule.getDateFormat();
            return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
        } catch (IllegalArgumentException e) {
            log.warn("单据号规则日期格式非法，回退 yyyyMMdd: bizKey={}, dateFormat={}",
                    rule.getBizKey(), rule.getDateFormat());
            return LocalDate.now().format(FALLBACK_DATE_FORMAT);
        }
    }
}
