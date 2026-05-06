package com.linan.system.spi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.spi.audit.OperLogSink;
import com.linan.core.spi.audit.OperationLog;
import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 操作日志 SPI 的系统侧实现
 *
 * <p>将 core 的 {@link OperationLog} 字段拷贝到 {@link SysOperLog} 落库，
 * 并承担 30 天前的历史日志清理，使 core 切面/清理任务与系统实体解耦。</p>
 *
 * @author BareZen Team
 */
@Component
@RequiredArgsConstructor
public class OperLogSinkImpl implements OperLogSink {

    private final ISysOperLogService operLogService;

    @Override
    public void save(OperationLog log) {
        SysOperLog entity = new SysOperLog();
        entity.setTitle(log.getTitle());
        entity.setBusinessType(log.getBusinessType());
        entity.setRequestMethod(log.getRequestMethod());
        entity.setOperUser(log.getOperUser());
        entity.setOperIp(log.getOperIp());
        entity.setOperUrl(log.getOperUrl());
        entity.setClassName(log.getClassName());
        entity.setMethodName(log.getMethodName());
        entity.setOperParam(log.getOperParam());
        entity.setJsonResult(log.getJsonResult());
        entity.setStatus(log.getStatus());
        entity.setErrorMsg(log.getErrorMsg());
        entity.setCostTime(log.getCostTime());
        entity.setOperTime(new Date());
        operLogService.save(entity);
    }

    @Override
    public long countBefore(LocalDateTime deadline) {
        return operLogService.count(new LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getCreateTime, deadline));
    }

    @Override
    public boolean removeBefore(LocalDateTime deadline) {
        return operLogService.remove(new LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getCreateTime, deadline));
    }
}