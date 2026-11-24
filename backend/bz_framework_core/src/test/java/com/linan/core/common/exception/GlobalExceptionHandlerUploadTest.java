package com.linan.core.common.exception;

import com.linan.core.common.api.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GlobalExceptionHandler 上传超限提示单元测试
 *
 * <p>上传超限请求在进入 Controller 之前就被 multipart 解析器拒绝，
 * 业务层的文件大小校验不会执行；若不单独拦截，用户只会看到通用兜底文案。</p>
 */
class GlobalExceptionHandlerUploadTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("上传超过容器大小上限：返回可读提示而非通用兜底文案")
    void maxUploadSizeExceeded_returnsReadableMessage() {
        Result<?> result = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(100L));

        assertEquals(400, result.getCode());
        assertEquals("上传文件大小超出限制，请压缩后重试", result.getMsg());
    }

    @Test
    @DisplayName("multipart 解析失败（超限的父类）：仍走兜底分支，不误判为成功")
    void multipartException_fallsBackToGenericError() {
        Result<?> result = handler.handleRuntimeException(
                new MultipartException("Maximum upload size exceeded"));

        assertEquals(500, result.getCode());
        assertEquals("服务器开小差了，请稍后再试", result.getMsg());
    }
}
