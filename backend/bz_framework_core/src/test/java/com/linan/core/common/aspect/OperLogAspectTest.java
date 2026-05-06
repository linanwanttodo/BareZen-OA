package com.linan.core.common.aspect;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.common.util.IpUtil;
import com.linan.core.spi.audit.OperationLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OperLogAspect 单元测试：业务失败（Result.error）应记为 status=1 并留痕
 */
@ExtendWith(MockitoExtension.class)
class OperLogAspectTest {

    @Mock
    private OperLogAsyncService operLogAsyncService;

    @Mock
    private IpUtil ipUtil;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    private OperLogAspect aspect;

    /** @OperLog 注解的桩方法，供 MethodSignature.getMethod() 返回真实 Method */
    @OperLog(title = "测试业务", businessType = 1)
    private Result<?> stubMethod() {
        return Result.success("ok");
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        aspect = new OperLogAspect(operLogAsyncService, ipUtil);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(getClass().getDeclaredMethod("stubMethod"));
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
    }

    /** 执行 around 并捕获异步保存的日志模型 */
    private OperationLog invokeAndCapture(Object result) throws Throwable {
        when(joinPoint.proceed()).thenReturn(result);
        aspect.around(joinPoint);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operLogAsyncService).saveOperLog(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("status 判定")
    class StatusJudgement {

        @Test
        @DisplayName("Result.success 记 status=0")
        void successResult_status0() throws Throwable {
            OperationLog log = invokeAndCapture(Result.success("ok"));

            assertEquals(0, log.getStatus());
            assertNull(log.getErrorMsg());
        }

        @Test
        @DisplayName("Result.error 业务失败记 status=1 且 errorMsg 留痕")
        void businessFailResult_status1() throws Throwable {
            OperationLog log = invokeAndCapture(Result.error("仅待审批状态的单据可删除"));

            assertEquals(1, log.getStatus());
            assertEquals("仅待审批状态的单据可删除", log.getErrorMsg());
            // 响应结果也留痕，便于事后追溯
            assertTrue(log.getJsonResult().contains("仅待审批状态的单据可删除"));
        }

        @Test
        @DisplayName("抛异常记 status=1 并重抛")
        void thrownException_status1() {
            try {
                when(joinPoint.proceed()).thenThrow(new RuntimeException("模拟异常"));
            } catch (Throwable ignored) {
            }

            assertThrows(RuntimeException.class, () -> aspect.around(joinPoint));

            ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
            verify(operLogAsyncService).saveOperLog(captor.capture());
            assertEquals(1, captor.getValue().getStatus());
            assertEquals("模拟异常", captor.getValue().getErrorMsg());
        }
    }
}