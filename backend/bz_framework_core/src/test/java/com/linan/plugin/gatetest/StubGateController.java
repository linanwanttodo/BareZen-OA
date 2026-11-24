package com.linan.plugin.gatetest;

import com.linan.core.common.api.Result;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 门控切面测试桩：位于 com.linan.plugin 包下并标注 @RestController，
 * 用于验证包推导（com.linan.plugin.gatetest -> com.linan.plugin.gatetest）与拦截行为
 */
@RestController
public class StubGateController {

    private final AtomicInteger callCount = new AtomicInteger();

    public Result<String> list() {
        callCount.incrementAndGet();
        return Result.success("ok");
    }

    public int getCallCount() {
        return callCount.get();
    }
}
