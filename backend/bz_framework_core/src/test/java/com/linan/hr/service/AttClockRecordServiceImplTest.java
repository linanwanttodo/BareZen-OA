package com.linan.hr.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.exception.BusinessException;
import com.linan.hr.entity.AttClockRecord;
import com.linan.hr.mapper.AttClockRecordMapper;
import com.linan.hr.service.impl.AttClockRecordServiceImpl;
import com.linan.system.service.ISysConfigService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AttClockRecordServiceImpl 单元测试
 *
 * <p>覆盖：上班打卡新建/迟到计算/宽容分钟/重复拒绝；下班打卡条件更新/早退计算/
 * 迟到早退合并异常/未上班拒绝/重复拒绝；规则缺失回退默认值；当月与今日查询；
 * 已批准请假/加班单覆盖时段豁免迟到早退（T1.6）。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class AttClockRecordServiceImplTest {

    private static final Long USER_ID = 100L;
    private static final String USER_NAME = "张三";

    @Mock
    private AttClockRecordMapper clockMapper;

    @Mock
    private ISysConfigService configService;

    @Mock
    private IAttLeaveRequestService leaveService;

    @Mock
    private IAttOvertimeRequestService overtimeService;

    private AttClockRecordServiceImpl clockService;

    @BeforeAll
    static void initTableInfoCache() {
        // 初始化实体列映射，使 LambdaQueryWrapper 在纯 Mockito 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttClockRecord.class);
    }

    @BeforeEach
    void setUp() {
        clockService = new AttClockRecordServiceImpl(configService, leaveService, overtimeService);
        ReflectionTestUtils.setField(clockService, "baseMapper", clockMapper);
    }

    /** 构造当日已有记录（默认仅上班打卡） */
    private AttClockRecord existingRecord(LocalDateTime in, LocalDateTime out, String status) {
        AttClockRecord record = new AttClockRecord();
        record.setId(1L);
        record.setUserId(USER_ID);
        record.setUserName(USER_NAME);
        record.setClockDate(in.toLocalDate());
        record.setClockInTime(in);
        record.setClockOutTime(out);
        record.setLateMinutes("late".equals(status) || "abnormal".equals(status) ? 30 : 0);
        record.setEarlyMinutes("early".equals(status) || "abnormal".equals(status) ? 60 : 0);
        record.setStatus(status);
        return record;
    }

    @Nested
    @DisplayName("clockIn() 上班打卡")
    class ClockIn {

        @Test
        @DisplayName("未登录直接拒绝")
        void nullUser_rejected() {
            assertThrows(BusinessException.class,
                    () -> clockService.clockIn(null, USER_NAME, LocalDateTime.now()));
        }

        @Test
        @DisplayName("整点打卡正常，迟到 0 分钟")
        void onTime_normal() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);
            when(configService.selectConfigByKey("hr.attendance.work_start_time")).thenReturn("09:00");
            when(configService.selectConfigByKey("hr.attendance.late_grace_minutes")).thenReturn("0");
            when(clockMapper.insert(any(AttClockRecord.class))).thenReturn(1);

            LocalDateTime now = LocalDateTime.of(2026, 3, 14, 9, 0);
            AttClockRecord result = clockService.clockIn(USER_ID, USER_NAME, now);

            assertEquals(LocalDate.of(2026, 3, 14), result.getClockDate());
            assertEquals(now, result.getClockInTime());
            assertEquals(0, result.getLateMinutes());
            assertEquals("normal", result.getStatus());

            ArgumentCaptor<AttClockRecord> captor = ArgumentCaptor.forClass(AttClockRecord.class);
            verify(clockMapper).insert(captor.capture());
            assertEquals(USER_ID, captor.getValue().getUserId());
            assertEquals(USER_NAME, captor.getValue().getUserName());
            assertEquals("normal", captor.getValue().getStatus());
        }

        @Test
        @DisplayName("09:30 打卡迟到 30 分钟")
        void late_calculated() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);
            when(configService.selectConfigByKey("hr.attendance.work_start_time")).thenReturn("09:00");
            when(configService.selectConfigByKey("hr.attendance.late_grace_minutes")).thenReturn("0");
            when(clockMapper.insert(any(AttClockRecord.class))).thenReturn(1);

            AttClockRecord result = clockService.clockIn(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 9, 30));

            assertEquals(30, result.getLateMinutes());
            assertEquals("late", result.getStatus());
        }

        @Test
        @DisplayName("宽容 5 分钟内不迟到，超出才记")
        void grace_absorbed() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);
            when(configService.selectConfigByKey("hr.attendance.work_start_time")).thenReturn("09:00");
            when(configService.selectConfigByKey("hr.attendance.late_grace_minutes")).thenReturn("5");
            when(clockMapper.insert(any(AttClockRecord.class))).thenReturn(1);

            // 09:04 在宽容内 → 不迟到
            AttClockRecord within = clockService.clockIn(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 9, 4));
            assertEquals(0, within.getLateMinutes());
            assertEquals("normal", within.getStatus());

            // 09:06 超出宽容 1 分钟 → 迟到 1 分钟
            AttClockRecord beyond = clockService.clockIn(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 9, 6));
            assertEquals(1, beyond.getLateMinutes());
            assertEquals("late", beyond.getStatus());
        }

        @Test
        @DisplayName("当日已有打卡记录，重复上班打卡拒绝")
        void duplicate_rejected() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(
                    existingRecord(LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> clockService.clockIn(USER_ID, USER_NAME, LocalDateTime.of(2026, 3, 14, 9, 30)));
            assertTrue(ex.getMessage().contains("重复"));
        }

        @Test
        @DisplayName("规则配置缺失时回退默认 09:00 / 0 宽容")
        void missingRule_fallsBackToDefault() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);
            when(configService.selectConfigByKey("hr.attendance.work_start_time")).thenReturn(null);
            when(configService.selectConfigByKey("hr.attendance.late_grace_minutes")).thenReturn(null);
            when(clockMapper.insert(any(AttClockRecord.class))).thenReturn(1);

            AttClockRecord result = clockService.clockIn(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 9, 10));

            assertEquals(10, result.getLateMinutes(), "无配置时按 09:00 基准计算");
            assertEquals("late", result.getStatus());
        }

        @Test
        @DisplayName("当日已批准请假覆盖上班时刻 → 迟到豁免")
        void approvedLeave_coversClockIn_lateExempted() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);
            when(configService.selectConfigByKey("hr.attendance.work_start_time")).thenReturn("09:00");
            when(configService.selectConfigByKey("hr.attendance.late_grace_minutes")).thenReturn("0");
            when(leaveService.count(any())).thenReturn(1L);
            when(clockMapper.insert(any(AttClockRecord.class))).thenReturn(1);

            AttClockRecord result = clockService.clockIn(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 10, 0));

            assertEquals(0, result.getLateMinutes(), "请假覆盖时段不记迟到");
            assertEquals("normal", result.getStatus());
        }

        @Test
        @DisplayName("无已批准请假单 → 迟到正常记录")
        void noApprovedLeave_lateRecorded() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);
            when(configService.selectConfigByKey("hr.attendance.work_start_time")).thenReturn("09:00");
            when(configService.selectConfigByKey("hr.attendance.late_grace_minutes")).thenReturn("0");
            when(clockMapper.insert(any(AttClockRecord.class))).thenReturn(1);

            AttClockRecord result = clockService.clockIn(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 10, 0));

            assertEquals(60, result.getLateMinutes());
            assertEquals("late", result.getStatus());
            // 豁免查询确实执行过且返回 0
            verify(leaveService).count(any());
        }
    }

    @Nested
    @DisplayName("clockOut() 下班打卡")
    class ClockOut {

        @Test
        @DisplayName("未上班打卡，下班打卡拒绝")
        void withoutClockIn_rejected() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> clockService.clockOut(USER_ID, USER_NAME, LocalDateTime.of(2026, 3, 14, 18, 0)));
            assertTrue(ex.getMessage().contains("先打卡上班"));
        }

        @Test
        @DisplayName("18:30 正常下班，保留正常状态")
        void onTime_normal() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);
            when(configService.selectConfigByKey("hr.attendance.work_end_time")).thenReturn("18:00");
            when(clockMapper.update(any(), any())).thenReturn(1);

            AttClockRecord result = clockService.clockOut(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 18, 30));

            assertEquals(0, result.getEarlyMinutes());
            assertEquals("normal", result.getStatus());
            assertEquals(LocalDateTime.of(2026, 3, 14, 18, 30), result.getClockOutTime());
        }

        @Test
        @DisplayName("17:30 下班早退 30 分钟")
        void early_calculated() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);
            when(configService.selectConfigByKey("hr.attendance.work_end_time")).thenReturn("18:00");
            when(clockMapper.update(any(), any())).thenReturn(1);

            AttClockRecord result = clockService.clockOut(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 17, 30));

            assertEquals(30, result.getEarlyMinutes());
            assertEquals("early", result.getStatus());
        }

        @Test
        @DisplayName("迟到且早退合并为异常状态")
        void lateAndEarly_abnormal() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 9, 30), null, "late");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);
            when(configService.selectConfigByKey("hr.attendance.work_end_time")).thenReturn("18:00");
            when(clockMapper.update(any(), any())).thenReturn(1);

            AttClockRecord result = clockService.clockOut(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 17, 0));

            assertEquals(60, result.getEarlyMinutes());
            assertEquals("abnormal", result.getStatus());
        }

        @Test
        @DisplayName("已打卡下班，重复下班打卡拒绝")
        void duplicate_rejected() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55),
                    LocalDateTime.of(2026, 3, 14, 18, 0), "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> clockService.clockOut(USER_ID, USER_NAME, LocalDateTime.of(2026, 3, 14, 19, 0)));
            assertTrue(ex.getMessage().contains("重复"));
        }

        @Test
        @DisplayName("已批准加班单覆盖下班时刻 → 早退豁免")
        void approvedOvertime_coversClockOut_earlyExempted() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);
            when(configService.selectConfigByKey("hr.attendance.work_end_time")).thenReturn("18:00");
            when(overtimeService.count(any())).thenReturn(1L);
            when(clockMapper.update(any(), any())).thenReturn(1);

            AttClockRecord result = clockService.clockOut(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 17, 30));

            assertEquals(0, result.getEarlyMinutes(), "加班时段内打卡下班不记早退");
            assertEquals("normal", result.getStatus());
        }

        @Test
        @DisplayName("已批准请假覆盖下班时刻 → 早退豁免")
        void approvedLeave_coversClockOut_earlyExempted() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);
            when(configService.selectConfigByKey("hr.attendance.work_end_time")).thenReturn("18:00");
            when(leaveService.count(any())).thenReturn(1L);
            when(clockMapper.update(any(), any())).thenReturn(1);

            AttClockRecord result = clockService.clockOut(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 17, 30));

            assertEquals(0, result.getEarlyMinutes(), "请假覆盖时段不记早退");
            assertEquals("normal", result.getStatus());
        }

        @Test
        @DisplayName("无批准单据覆盖 → 早退正常记录")
        void noApproval_earlyRecorded() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);
            when(configService.selectConfigByKey("hr.attendance.work_end_time")).thenReturn("18:00");
            when(clockMapper.update(any(), any())).thenReturn(1);

            AttClockRecord result = clockService.clockOut(USER_ID, USER_NAME,
                    LocalDateTime.of(2026, 3, 14, 17, 30));

            assertEquals(30, result.getEarlyMinutes());
            assertEquals("early", result.getStatus());
            verify(leaveService).count(any());
            verify(overtimeService).count(any());
        }

        @Test
        @DisplayName("并发冲突（条件更新 0 行）拒绝并提示刷新")
        void concurrentUpdate_conflict() {
            AttClockRecord existing = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(existing);
            when(configService.selectConfigByKey("hr.attendance.work_end_time")).thenReturn("18:00");
            when(clockMapper.update(any(), any())).thenReturn(0);

            assertThrows(BusinessException.class,
                    () -> clockService.clockOut(USER_ID, USER_NAME, LocalDateTime.of(2026, 3, 14, 18, 30)));
        }
    }

    @Nested
    @DisplayName("查询")
    class Query {

        @Test
        @DisplayName("listMonth 按用户与月份范围过滤，日期升序")
        void listMonth_filtersByUserAndMonth() {
            AttClockRecord record = existingRecord(
                    LocalDateTime.of(2026, 3, 5, 8, 55), null, "normal");
            when(clockMapper.selectList(any())).thenReturn(List.of(record));

            List<AttClockRecord> result = clockService.listMonth(USER_ID, YearMonth.of(2026, 3));

            assertEquals(1, result.size());
            ArgumentCaptor<LambdaQueryWrapper<AttClockRecord>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(clockMapper).selectList(captor.capture());
            String sql = captor.getValue().getSqlSegment();
            assertTrue(sql.contains("user_id"), "必须按用户过滤");
            assertTrue(sql.contains("clock_date"), "必须按日期范围过滤");
        }

        @Test
        @DisplayName("today 返回当日记录，无记录为 null")
        void today_returnsRecordOrNull() {
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(null);
            assertNull(clockService.today(USER_ID, LocalDateTime.of(2026, 3, 14, 10, 0)));

            AttClockRecord record = existingRecord(
                    LocalDateTime.of(2026, 3, 14, 8, 55), null, "normal");
            when(clockMapper.selectOne(any(), anyBoolean())).thenReturn(record);
            assertEquals(record, clockService.today(USER_ID, LocalDateTime.of(2026, 3, 14, 10, 0)));
        }
    }
}