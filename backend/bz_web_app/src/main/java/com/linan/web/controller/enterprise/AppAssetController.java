package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.asset.dto.AssetBorrowRecordDTO;
import com.linan.asset.dto.AssetItemPublicDTO;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.service.IAssetBorrowRecordService;
import com.linan.asset.service.IAssetCategoryService;
import com.linan.asset.service.IAssetItemService;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageParamGuard;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资产借用 Controller（用户前台 /api/app/v1/assets）
 *
 * <p>员工自助浏览可借资产、申请借用、归还。与管理后台的资产台账接口
 * （/api/asset/v1/items）分离：这里无需管理权限，仅要求登录；
 * 借用人身份由服务端强制注入当前登录用户，接口不接收任何身份参数。</p>
 *
 * <p>借出与归还复用管理端同一 Service 方法
 * （{@link IAssetItemService#borrowAsset} 原子条件更新 WHERE status='0' 防并发重复借用、
 * {@link IAssetItemService#returnAsset} 状态回库并同步借用记录），
 * 与管理端口径完全一致；用户端重载仅在借用记录上额外落 borrowerId，
 * 供「我的借用」归属过滤与归还归属校验使用。</p>
 *
 * <p>可借资产查询仅返回在库（status=0）资产，且映射为公开视图
 * {@link AssetItemPublicDTO}，不泄露购置价格、使用人等内部管理字段。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "资产借用", description = "用户前台 - 资产借用")
@RestController
@RequestMapping("/api/app/v1/assets")
@RequiredArgsConstructor
public class AppAssetController {

    /** 资产状态：在库（0=在库 1=使用中 2=维修中 3=已报废） */
    private static final String ITEM_STATUS_IN_STOCK = "0";

    /** 借用记录状态：借用中（1=已归还） */
    private static final String RECORD_STATUS_BORROWING = "0";

    private final IAssetItemService assetService;

    private final IAssetCategoryService categoryService;

    private final IAssetBorrowRecordService borrowRecordService;

    /**
     * 资产分类列表（仅启用状态，按排序号升序）
     * GET /api/app/v1/assets/categories
     */
    @Operation(summary = "资产分类列表")
    @GetMapping("/categories")
    public Result<List<AssetCategory>> categories() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        // 复用管理端同口径查询：启用分类按排序号升序
        return Result.success(categoryService.listCategories(1));
    }

    /**
     * 可借用资产分页列表（仅返回在库资产，分类筛选 + 名称关键字）
     * GET /api/app/v1/assets/items
     *
     * <p>状态过滤服务端强制注入（仅 status=0 在库），接口不接收 status 参数；
     * 分类与名称筛选沿用管理端同名列查询口径；结果映射为公开视图，
     * 不返回购置价格、使用部门、使用人等内部管理字段。</p>
     */
    @Operation(summary = "可借用资产分页列表")
    @GetMapping("/items")
    public Result<PageResult<AssetItemPublicDTO>> items(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String assetName) {

        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<AssetItem> wrapper = new LambdaQueryWrapper<AssetItem>()
                .eq(AssetItem::getStatus, ITEM_STATUS_IN_STOCK);
        if (categoryId != null) {
            wrapper.eq(AssetItem::getCategoryId, categoryId);
        }
        if (assetName != null && !assetName.isEmpty()) {
            wrapper.like(AssetItem::getAssetName, assetName);
        }
        wrapper.orderByAsc(AssetItem::getAssetName).orderByAsc(AssetItem::getId);
        IPage<AssetItem> result = assetService.page(new Page<>(page, pageSize), wrapper);
        IPage<AssetItemPublicDTO> mapped = result.convert(this::toPublicDTO);
        return Result.page(mapped);
    }

    /**
     * 可借用资产详情（仅在库资产可见）
     * GET /api/app/v1/assets/items/{id}
     */
    @Operation(summary = "资产详情")
    @GetMapping("/items/{id}")
    public Result<AssetItemPublicDTO> itemDetail(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        AssetItem item = assetService.getById(id);
        if (item == null || !ITEM_STATUS_IN_STOCK.equals(item.getStatus())) {
            return Result.error("资产不存在或已不可借用");
        }
        return Result.success(toPublicDTO(item));
    }

    /**
     * 申请借用（服务端强制当前登录用户为借用人）
     * POST /api/app/v1/assets/items/{id}/borrow
     *
     * <p>复用管理端同一借出 Service 方法：在库校验 + 原子状态流转
     * （WHERE status='0'），并发借用仅首个生效；借用记录落当前用户ID，
     * 归属不可伪造。预计归还时间与备注为可选参数，口径与管理端一致。</p>
     */
    @Operation(summary = "申请借用资产")
    @OperLog(title = "资产借用", businessType = 2)
    @PostMapping("/items/{id}/borrow")
    public Result<Void> borrow(@PathVariable Long id,
                               @RequestParam(required = false) String expectReturnTime,
                               @RequestParam(required = false) String remark) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        // 借用人身份服务端强制注入，不信任任何前端传参
        String username = AuthInterceptor.getCurrentUsername();
        String borrowerName = username != null ? username : "用户" + userId;
        assetService.borrowAsset(id, userId, borrowerName, expectReturnTime, remark);
        return Result.success(null);
    }

    /**
     * 我的借用记录分页（含归还状态，附资产编号与名称）
     * GET /api/app/v1/assets/my-borrows
     *
     * <p>归属条件由服务端强制注入当前登录用户，接口不接收 userId 参数。
     * 资产名称为列表展示冗余，资产已删除时置空。</p>
     */
    @Operation(summary = "我的借用记录分页")
    @GetMapping("/my-borrows")
    public Result<PageResult<AssetBorrowRecordDTO>> myBorrows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<AssetBorrowRecord> wrapper = new LambdaQueryWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getBorrowerId, userId)
                .orderByDesc(AssetBorrowRecord::getCreateTime)
                .orderByDesc(AssetBorrowRecord::getId);
        IPage<AssetBorrowRecord> result = borrowRecordService.page(new Page<>(page, pageSize), wrapper);
        Map<Long, AssetItem> assetMap = loadAssetMap(result);
        return Result.page(result.convert(record -> toBorrowDTO(record, assetMap)));
    }

    /**
     * 归还我的借用（仅本人，复用管理端归还 Service）
     * PUT /api/app/v1/assets/my-borrows/{id}/return
     *
     * <p>{@code id} 为借用记录ID。归属校验：仅借用人本人可归还；
     * 归还动作复用管理端 {@link IAssetItemService#returnAsset}，
     * 资产状态原子回库并同步借用记录归还时间，口径与管理端一致。</p>
     */
    @Operation(summary = "归还借用资产")
    @OperLog(title = "资产借用", businessType = 2)
    @PutMapping("/my-borrows/{id}/return")
    public Result<Void> returnBorrow(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        AssetBorrowRecord record = borrowRecordService.getById(id);
        if (record == null) {
            return Result.error("借用记录不存在");
        }
        // 归属校验：仅借用人本人可归还（管理端代借记录无 borrowerId，不可由用户端归还）
        if (record.getBorrowerId() == null || !userId.equals(record.getBorrowerId())) {
            return Result.error("无权归还该借用记录");
        }
        if (!RECORD_STATUS_BORROWING.equals(record.getStatus())) {
            return Result.error("该借用记录已归还，请勿重复操作");
        }
        assetService.returnAsset(record.getAssetId());
        return Result.success(null);
    }

    /** 批量加载当前页涉及资产的编号/名称映射（已删除资产自然缺省，展示置空） */
    private Map<Long, AssetItem> loadAssetMap(IPage<AssetBorrowRecord> page) {
        List<Long> assetIds = page.getRecords().stream()
                .map(AssetBorrowRecord::getAssetId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        return assetService.listByIds(assetIds).stream()
                .collect(Collectors.toMap(AssetItem::getId, Function.identity()));
    }

    private AssetBorrowRecordDTO toBorrowDTO(AssetBorrowRecord record, Map<Long, AssetItem> assetMap) {
        AssetBorrowRecordDTO dto = new AssetBorrowRecordDTO();
        dto.setId(record.getId());
        dto.setAssetId(record.getAssetId());
        dto.setBorrowerId(record.getBorrowerId());
        dto.setBorrowerName(record.getBorrowerName());
        dto.setBorrowTime(record.getBorrowTime());
        dto.setExpectReturnTime(record.getExpectReturnTime());
        dto.setReturnTime(record.getReturnTime());
        dto.setRemark(record.getRemark());
        dto.setStatus(record.getStatus());
        AssetItem asset = assetMap.get(record.getAssetId());
        if (asset != null) {
            dto.setAssetNo(asset.getAssetNo());
            dto.setAssetName(asset.getAssetName());
        }
        return dto;
    }

    /** 实体转公开视图：仅映射用户端可见字段，屏蔽价格/使用人/部门/报废等管理字段 */
    private AssetItemPublicDTO toPublicDTO(AssetItem item) {
        AssetItemPublicDTO dto = new AssetItemPublicDTO();
        dto.setId(item.getId());
        dto.setAssetNo(item.getAssetNo());
        dto.setAssetName(item.getAssetName());
        dto.setCategoryId(item.getCategoryId());
        dto.setCategory(item.getCategory());
        dto.setBrand(item.getBrand());
        dto.setModel(item.getModel());
        dto.setLocation(item.getLocation());
        dto.setStatus(item.getStatus());
        return dto;
    }
}
