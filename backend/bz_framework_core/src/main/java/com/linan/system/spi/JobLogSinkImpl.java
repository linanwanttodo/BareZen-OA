package com.linan.system.spi;

import com.linan.core.spi.audit.JobExecutionLog;
import com.linan.core.spi.audit.JobLogSink;
import com.linan.system.entity.SysJobLog;
import com.linan.system.mapper.SysJobLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 定时任务日志 SPI 的系统侧实现
 *
 * <p>将 core 的 {@link JobExecutionLog} 拷贝到 {@link SysJobLog} 落库，
 * 使 Quartz 执行器不再直接持有系统 mapper。</p>
 *
 * @author BareZen Team
 */
@Component
@RequiredArgsConstructor
public class JobLogSinkImpl implements JobLogSink {

    private final SysJobLogMapper jobLogMapper;

    @Override
    public void save(JobExecutionLog log) {
        SysJobLog entity = new SysJobLog();
        entity.setJobId(log.getJobId());
        entity.setJobName(log.getJobName());
        entity.setJobGroup(log.getJobGroup());
        entity.setInvokeTarget(log.getInvokeTarget());
        entity.setCronExpression(log.getCronExpression());
        entity.setStatus(log.getStatus());
        entity.setErrorMsg(log.getErrorMsg());
        entity.setStartTime(log.getStartTime());
        entity.setStopTime(log.getStopTime());
        entity.setDuration(log.getDuration());
        jobLogMapper.insert(entity);
    }
}