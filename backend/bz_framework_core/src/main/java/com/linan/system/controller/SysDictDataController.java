package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysDictData;
import com.linan.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数据字典 Controller
 *
 * <p>提供字典数据的分页查询与增删改（管理端「字典管理 - 数据」Tab），
 * 写操作后同步清除对应类型的 Redis 缓存，避免下拉数据不刷新。</p>
 */
@Tag(name = "字典管理", description = "系统管理 - 数据字典查询与维护")
@RestController
@RequestMapping("/api/system/v1/dict/data")
@RequiredArgsConstructor
public class SysDictDataController extends BaseController<SysDictData, ISysDictDataService> {

    private final ISysDictDataService dictDataService;

    @Override
    protected ISysDictDataService getService() {
        return dictDataService;
    }

    /**
     * 根据字典类型查询字典数据列表（带缓存）
     * GET /api/system/v1/dict/data/type/{dictType}
     */
    @RequirePermission("system:dict:list")
    @Operation(summary = "按类型查询字典数据")
    @GetMapping("/type/{dictType}")
    public Result<List<SysDictData>> getByType(@PathVariable String dictType) {
        List<SysDictData> list = dictDataService.selectDictDataByType(dictType);
        return Result.success(list);
    }

    /**
     * 分页查询字典数据（支持按类型/标签/状态过滤）
     * GET /api/system/v1/dict/data/page?pageNum=1&pageSize=10&dictType=sys_user_sex&dictLabel=&status=
     */
    @RequirePermission("system:dict:list")
    @Operation(summary = "分页查询字典数据")
    @GetMapping("/page")
    public Result<PageResult<SysDictData>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictLabel,
            @RequestParam(required = false) String status) {

        Page<SysDictData> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (dictType != null && !dictType.isEmpty()) {
            wrapper.eq(SysDictData::getDictType, dictType);
        }
        if (dictLabel != null && !dictLabel.isEmpty()) {
            wrapper.like(SysDictData::getDictLabel, dictLabel);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysDictData::getStatus, status);
        }
        wrapper.orderByAsc(SysDictData::getDictSort);
        IPage<SysDictData> result = dictDataService.page(page, wrapper);
        return Result.page(result);
    }

    @RequirePermission("system:dict:list")
    @Operation(summary = "查询字典数据详情")
    @GetMapping("/{id}")
    @Override
    public Result<SysDictData> getById(@PathVariable Long id) {
        return super.getById(id);
    }

    @Operation(summary = "新增字典数据")
    @OperLog(title = "字典数据", businessType = 1)
    @RequirePermission("system:dict:add")
    @PostMapping
    @Override
    public Result<Void> save(@jakarta.validation.Valid @RequestBody SysDictData entity) {
        Result<Void> r = super.save(entity);
        if (r.getCode() == 200 && entity.getDictType() != null) {
            dictDataService.clearDictCache(entity.getDictType());
        }
        return r;
    }

    @Operation(summary = "更新字典数据")
    @OperLog(title = "字典数据", businessType = 2)
    @RequirePermission("system:dict:edit")
    @PutMapping
    @Override
    public Result<Void> update(@jakarta.validation.Valid @RequestBody SysDictData entity) {
        Result<Void> r = super.update(entity);
        if (r.getCode() == 200 && entity.getDictType() != null) {
            dictDataService.clearDictCache(entity.getDictType());
        }
        return r;
    }

    @Operation(summary = "删除字典数据")
    @OperLog(title = "字典数据", businessType = 3)
    @RequirePermission("system:dict:delete")
    @DeleteMapping("/{id}")
    @Override
    public Result<Void> delete(@PathVariable Long id) {
        SysDictData old = dictDataService.getById(id);
        Result<Void> r = super.delete(id);
        if (r.getCode() == 200 && old != null && old.getDictType() != null) {
            dictDataService.clearDictCache(old.getDictType());
        }
        return r;
    }
}