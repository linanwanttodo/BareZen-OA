package com.linan.system.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysDictType;
import com.linan.system.service.ISysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "字典管理", description = "字典类型 CRUD")
@RestController
@RequestMapping("/api/system/v1/dict/types")
public class SysDictTypeController extends BaseController<SysDictType, ISysDictTypeService> {
    @Autowired private ISysDictTypeService dictTypeService;
    @Override protected ISysDictTypeService getService() { return dictTypeService; }

    @RequirePermission("system:dict:list")
    @Operation(summary = "分页查询字典类型")
    @GetMapping("/page")
    @Override
    public Result<PageResult<SysDictType>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return super.list(pageNum, pageSize);
    }

    @RequirePermission("system:dict:list")
    @Operation(summary = "查询字典类型详情")
    @GetMapping("/{id}")
    @Override
    public Result<SysDictType> getById(@PathVariable Long id) {
        return super.getById(id);
    }

    @Operation(summary = "新增字典类型")
    @OperLog(title = "字典类型", businessType = 1)
    @RequirePermission("system:dict:add")
    @PostMapping
    @Override
    public Result<Void> save(@jakarta.validation.Valid @RequestBody SysDictType entity) {
        return super.save(entity);
    }

    @Operation(summary = "更新字典类型")
    @OperLog(title = "字典类型", businessType = 2)
    @RequirePermission("system:dict:edit")
    @PutMapping
    @Override
    public Result<Void> update(@jakarta.validation.Valid @RequestBody SysDictType entity) {
        return super.update(entity);
    }

    @Operation(summary = "删除字典类型")
    @OperLog(title = "字典类型", businessType = 3)
    @RequirePermission("system:dict:delete")
    @DeleteMapping("/{id}")
    @Override
    public Result<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }
}
