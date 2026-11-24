package com.linan.asset.controller;

import com.linan.asset.service.IAssetBorrowRecordService;
import com.linan.asset.service.IAssetItemService;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 资产借用/归还 Controller 单元测试
 *
 * <p>业务逻辑已下沉至 AssetItemServiceImpl（含 @Transactional），
 * Controller 仅做委托。本测试覆盖成功/异常两条路径。</p>
 */
@ExtendWith(MockitoExtension.class)
class AssetItemBorrowControllerTest {

    @Mock
    private IAssetItemService assetService;

    @Mock
    private IAssetBorrowRecordService borrowRecordService;

    @InjectMocks
    private AssetItemController controller;

    @Test
    @DisplayName("借用：成功委托到 service 返回 200")
    void borrow_success() {
        doNothing().when(assetService).borrowAsset(eq(10L), eq("张三"), eq("2026-08-30"), eq("临时借用"));

        Result<Void> result = controller.borrow(10L, "张三", "2026-08-30", "临时借用");

        assertEquals(200, result.getCode());
        verify(assetService).borrowAsset(10L, "张三", "2026-08-30", "临时借用");
    }

    @Test
    @DisplayName("借用：非在库资产 service 抛 BusinessException")
    void borrow_notInStock_throws() {
        doThrow(new BusinessException("仅限在库资产借用"))
                .when(assetService).borrowAsset(eq(10L), anyString(), any(), any());

        assertThrows(BusinessException.class,
                () -> controller.borrow(10L, "张三", null, null));
    }

    @Test
    @DisplayName("借用：并发冲突 service 抛 BusinessException")
    void borrow_concurrentConflict_throws() {
        doThrow(new BusinessException("该资产已被并发借用"))
                .when(assetService).borrowAsset(eq(10L), anyString(), any(), any());

        assertThrows(BusinessException.class,
                () -> controller.borrow(10L, "张三", null, null));
    }

    @Test
    @DisplayName("归还：成功委托到 service 返回 200")
    void returnAsset_success() {
        doNothing().when(assetService).returnAsset(10L);

        Result<Void> result = controller.returnAsset(10L);

        assertEquals(200, result.getCode());
        verify(assetService).returnAsset(10L);
    }

    @Test
    @DisplayName("归还：非使用中资产 service 抛 BusinessException")
    void returnAsset_notInUse_throws() {
        doThrow(new BusinessException("仅使用中的资产可归还"))
                .when(assetService).returnAsset(10L);

        assertThrows(BusinessException.class,
                () -> controller.returnAsset(10L));
    }

    @Test
    @DisplayName("报废：成功委托到 service 返回 200")
    void scrap_success() {
        doNothing().when(assetService).scrapAsset(10L);

        Result<Void> result = controller.scrap(10L);

        assertEquals(200, result.getCode());
        verify(assetService).scrapAsset(10L);
    }
}
