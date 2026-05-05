package com.linan.asset.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.service.IAssetBorrowRecordService;
import com.linan.asset.service.IAssetItemService;
import com.linan.core.common.api.Result;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资产借用/归还 Controller 单元测试（2.2）
 *
 * <p>覆盖：在库可借（原子条件更新防并发）、非在库拒绝、已报废拒绝、重复借用拒绝、
 * 归还流转（使用中→在库，借用记录置已归还）、非使用中归还拒绝。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class AssetItemBorrowControllerTest {

    @Mock
    private IAssetItemService assetService;

    @Mock
    private IAssetBorrowRecordService borrowRecordService;

    @InjectMocks
    private AssetItemController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AssetItem.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AssetBorrowRecord.class);
    }

    private AssetItem item(Long id, String status) {
        AssetItem i = new AssetItem();
        i.setId(id);
        i.setStatus(status);
        return i;
    }

    @Test
    @DisplayName("借用：在库资产借用成功，状态原子流转为使用中并生成借用记录")
    void borrow_inStock_success() {
        AssetItem item = item(10L, "0");
        when(assetService.getById(10L)).thenReturn(item);
        when(assetService.update(any(Wrapper.class))).thenReturn(true);
        when(borrowRecordService.save(any(AssetBorrowRecord.class))).thenReturn(true);

        Result<Void> result = controller.borrow(10L, "张三", "2026-08-30", "临时借用");

        assertEquals(200, result.getCode());
        // 原子条件更新：WHERE status='0'，SET status='1' + holderName
        ArgumentCaptor<LambdaUpdateWrapper<AssetItem>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(assetService).update(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        String sqlSet = captor.getValue().getSqlSet();
        Map<String, Object> params = captor.getValue().getParamNameValuePairs();
        assertTrue(sql.contains("status"), "借用应限定在库状态");
        assertTrue(sqlSet.contains("holder_name") || sqlSet.contains("holderName"),
                "SET 应包含使用人，实际: " + sqlSet);
        assertTrue(params.containsValue("1"), "借用后状态应为使用中");
        // 借用记录：status=0 借用中，borrowerName=张三
        ArgumentCaptor<AssetBorrowRecord> recCaptor = ArgumentCaptor.forClass(AssetBorrowRecord.class);
        verify(borrowRecordService).save(recCaptor.capture());
        AssetBorrowRecord record = recCaptor.getValue();
        assertEquals(10L, record.getAssetId());
        assertEquals("张三", record.getBorrowerName());
        assertEquals("0", record.getStatus());
        assertNotNull(record.getBorrowTime());
    }

    @Test
    @DisplayName("借用：非在库资产（使用中）拒绝")
    void borrow_notInStock_rejected() {
        when(assetService.getById(10L)).thenReturn(item(10L, "1"));

        Result<Void> result = controller.borrow(10L, "张三", null, null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("在库"));
        verify(assetService, never()).update(any());
    }

    @Test
    @DisplayName("借用：已报废资产拒绝")
    void borrow_scrapped_rejected() {
        when(assetService.getById(10L)).thenReturn(item(10L, "3"));

        Result<Void> result = controller.borrow(10L, "张三", null, null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("在库"));
        verify(assetService, never()).update(any());
    }

    @Test
    @DisplayName("借用：并发冲突（原子更新 0 行）拒绝")
    void borrow_concurrentConflict_rejected() {
        when(assetService.getById(10L)).thenReturn(item(10L, "0"));
        when(assetService.update(any(Wrapper.class))).thenReturn(false);

        Result<Void> result = controller.borrow(10L, "张三", null, null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("并发"));
        verify(borrowRecordService, never()).save(any());
    }

    @Test
    @DisplayName("归还：使用中资产归还成功，置回在库并清空使用人、借用记录置已归还")
    void returnAsset_inUse_success() {
        when(assetService.getById(10L)).thenReturn(item(10L, "1"));
        when(assetService.update(any(Wrapper.class))).thenReturn(true);
        when(borrowRecordService.update(any(Wrapper.class))).thenReturn(true);

        Result<Void> result = controller.returnAsset(10L);

        assertEquals(200, result.getCode());
        // 资产原子更新：WHERE status='1'，SET status='0'
        ArgumentCaptor<LambdaUpdateWrapper<AssetItem>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(assetService).update(captor.capture());
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("0"));
        // 借用记录置已归还
        ArgumentCaptor<LambdaUpdateWrapper<AssetBorrowRecord>> recCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(borrowRecordService).update(recCaptor.capture());
        LambdaUpdateWrapper<AssetBorrowRecord> recWrapper = recCaptor.getValue();
        assertTrue(recWrapper.getParamNameValuePairs().containsValue("1"));
        assertTrue(recWrapper.getParamNameValuePairs().containsKey("return_time")
                || recWrapper.getSqlSet().contains("return_time"));
    }

    @Test
    @DisplayName("归还：非使用中资产拒绝")
    void returnAsset_notInUse_rejected() {
        when(assetService.getById(10L)).thenReturn(item(10L, "0"));

        Result<Void> result = controller.returnAsset(10L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("使用中"));
        verify(assetService, never()).update(any());
    }
}