package com.linan.core.common.aspect;

import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志异步写入服务
 *
 * <p>将操作日志的持久化操作从 AOP 切面中分离出来异步执行，
 * 避免日志写入耗时影响 API 响应时间。</p>
 */
@Slf4j
@Service
public class OperLogAsyncService {

    @Autowired
    private ISysOperLogService operLogService;

    /**
     * 异步保存操作日志
     *
     * @param logEntity 操作日志实体
     */
    @Async
    public void saveOperLog(SysOperLog logEntity) {
        try {
            operLogService.save(logEntity);
        } catch (Exception e) {
            log.error("异步保存操作日志失败", e);
        }
    }
}
