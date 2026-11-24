package com.linan.plugin.masterdata;

import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.masterdata.entity.DocNumberRule;
import com.linan.plugin.masterdata.mapper.DocNumberRuleMapper;
import com.linan.plugin.masterdata.service.impl.DocNumberServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 单据号服务单元测试
 *
 * <p>覆盖：格式拼接（SUP20261124-0001）、跨日序号重置、步长累计、
 * 规则缺失/前缀缺失/并发冲突的业务异常、非法日期格式回退。
 * 并发不重号的真库验证见 bz_web_app 的 DocNumberServiceConcurrencyTest。</p>
 */
@ExtendWith(MockitoExtension.class)
class DocNumberServiceImplTest {

    @Mock
    private DocNumberRuleMapper ruleMapper;

    private final DateTimeFormatter today = DateTimeFormatter.ofPattern("yyyyMMdd");

    private DocNumberServiceImpl newService() {
        return new DocNumberServiceImpl(ruleMapper);
    }

    private DocNumberRule rule(String bizKey, String prefix, String dateFormat,
                               Long currentSeq, Integer step, String lastDate) {
        DocNumberRule rule = new DocNumberRule();
        rule.setId(1L);
        rule.setBizKey(bizKey);
        rule.setPrefix(prefix);
        rule.setDateFormat(dateFormat);
        rule.setCurrentSeq(currentSeq);
        rule.setStep(step);
        rule.setLastDate(lastDate);
        return rule;
    }

    @Test
    @DisplayName("同日连续发号：序号按步长累计，格式为 前缀+日期-4位序号")
    void nextNumber_sameDate_incrementsAndFormats() {
        String dateStr = LocalDate.now().format(today);
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", "SUP", "yyyyMMdd", 3L, 1, dateStr));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        String number = newService().nextNumber("SUP");

        assertEquals("SUP" + dateStr + "-0004", number);
        verify(ruleMapper).updateSeq(1L, 3L, dateStr, 4L, dateStr);
    }

    @Test
    @DisplayName("首次发号：current_seq=0 且无 last_date，产出 0001")
    void nextNumber_firstIssue_produces0001() {
        String dateStr = LocalDate.now().format(today);
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", "SUP", "yyyyMMdd", 0L, 1, null));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        assertEquals("SUP" + dateStr + "-0001", newService().nextNumber("SUP"));
    }

    @Test
    @DisplayName("跨日发号：last_date 与今天不同，序号从 0 重置")
    void nextNumber_dateSegmentChanged_resetsSeq() {
        String dateStr = LocalDate.now().format(today);
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", "SUP", "yyyyMMdd", 99L, 1, "20200101"));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        assertEquals("SUP" + dateStr + "-0001", newService().nextNumber("SUP"));
        verify(ruleMapper).updateSeq(1L, 99L, "20200101", 1L, dateStr);
    }

    @Test
    @DisplayName("步长大于 1：按步长跳跃发号")
    void nextNumber_stepApplies() {
        String dateStr = LocalDate.now().format(today);
        when(ruleMapper.selectByBizKeyForUpdate("PO"))
                .thenReturn(rule("PO", "PO", "yyyyMMdd", 10L, 5, dateStr));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        assertEquals("PO" + dateStr + "-0015", newService().nextNumber("PO"));
    }

    @Test
    @DisplayName("日期段格式非 yyyyMMdd：按规则配置格式化")
    void nextNumber_customDateFormat() {
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", "SUP", "yyyy-MM-dd", 0L, 1, null));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertEquals("SUP" + dateStr + "-0001", newService().nextNumber("SUP"));
    }

    @Test
    @DisplayName("并发冲突：乐观护栏 updated=0 抛业务异常，不产出重复单据号")
    void nextNumber_optimisticConflict_throws() {
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", "SUP", "yyyyMMdd", 0L, 1, null));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(0);

        assertThrows(BusinessException.class, () -> newService().nextNumber("SUP"));
    }

    @Test
    @DisplayName("规则不存在抛业务异常")
    void nextNumber_ruleMissing_throws() {
        when(ruleMapper.selectByBizKeyForUpdate("NOTEXIST")).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> newService().nextNumber("NOTEXIST"));
        assertTrue(e.getMessage().contains("NOTEXIST"));
    }

    @Test
    @DisplayName("业务键为空抛业务异常，不触发锁查询")
    void nextNumber_blankBizKey_throws() {
        assertThrows(BusinessException.class, () -> newService().nextNumber("  "));
        assertThrows(BusinessException.class, () -> newService().nextNumber(null));
        verifyNoInteractions(ruleMapper);
    }

    @Test
    @DisplayName("前缀未配置抛业务异常")
    void nextNumber_blankPrefix_throws() {
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", " ", "yyyyMMdd", 0L, 1, null));

        assertThrows(BusinessException.class, () -> newService().nextNumber("SUP"));
    }

    @Test
    @DisplayName("非法日期格式回退 yyyyMMdd，不阻断发号")
    void nextNumber_invalidDateFormat_fallsBack() {
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", "SUP", "invalid-pattern!", 0L, 1, null));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        ArgumentCaptor<String> dateCaptor = ArgumentCaptor.forClass(String.class);
        String number = newService().nextNumber("SUP");
        verify(ruleMapper).updateSeq(anyLong(), anyLong(), any(), anyLong(), dateCaptor.capture());

        assertEquals(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                dateCaptor.getValue());
        assertEquals("SUP" + dateCaptor.getValue() + "-0001", number);
    }

    @Test
    @DisplayName("step 配置非法（<=0 或 null）按 1 处理")
    void nextNumber_invalidStep_defaultsToOne() {
        String dateStr = LocalDate.now().format(today);
        when(ruleMapper.selectByBizKeyForUpdate("SUP"))
                .thenReturn(rule("SUP", "SUP", "yyyyMMdd", 0L, 0, null));
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        assertEquals("SUP" + dateStr + "-0001", newService().nextNumber("SUP"));
    }

    @Test
    @DisplayName("last_date 同日但 current_seq 为 null：按 0 起发，不抛 NPE")
    void nextNumber_nullCurrentSeq_treatedAsZero() {
        String dateStr = LocalDate.now().format(today);
        DocNumberRule rule = rule("SUP", "SUP", "yyyyMMdd", null, 1, dateStr);
        when(ruleMapper.selectByBizKeyForUpdate("SUP")).thenReturn(rule);
        when(ruleMapper.updateSeq(anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);

        assertEquals("SUP" + dateStr + "-0001", newService().nextNumber("SUP"));
        verify(ruleMapper).updateSeq(1L, 0L, dateStr, 1L, dateStr);
    }
}
