package com.linan.core.quartz;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ScheduleRunnable 单元测试
 *
 * <p>验证从 JobKey 名称解析真实 jobId 的逻辑：
 * 数字名称应解析为对应 Long，非数字名称回退 hashCode。</p>
 */
class ScheduleRunnableTest {

    private JobExecutionContext mockContext(String keyName) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getKey()).thenReturn(JobKey.jobKey(keyName));
        when(context.getJobDetail()).thenReturn(jobDetail);
        return context;
    }

    @Test
    void parseJobIdFromKey_数字名称解析为真实ID() {
        JobExecutionContext context = mockContext("42");
        assertEquals(42L, ScheduleRunnable.parseJobIdFromKey(context));
    }

    @Test
    void parseJobIdFromKey_长ID解析() {
        JobExecutionContext context = mockContext("9876543210");
        assertEquals(9876543210L, ScheduleRunnable.parseJobIdFromKey(context));
    }

    @Test
    void parseJobIdFromKey_非数字名称回退hashCode() {
        JobExecutionContext context = mockContext("cleanTask");
        long expected = (long) (JobKey.jobKey("cleanTask").hashCode() & 0x7fffffff);
        assertEquals(expected, ScheduleRunnable.parseJobIdFromKey(context));
    }

    @Test
    void parseJobIdFromKey_空名称回退hashCode() {
        JobExecutionContext context = mockContext("");
        long expected = (long) (JobKey.jobKey("").hashCode() & 0x7fffffff);
        assertEquals(expected, ScheduleRunnable.parseJobIdFromKey(context));
    }
}
