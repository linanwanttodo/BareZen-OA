package com.linan.system.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysDictData;
import com.linan.system.service.ISysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典 Controller
 */
@RestController
@RequestMapping("/api/system/v1/dict/data")
public class SysDictDataController {

    @Autowired
    private ISysDictDataService dictDataService;

    /**
     * 根据字典类型查询字典数据列表（带缓存）
     * GET /api/system/v1/dict/data/type/{dictType}
     */
    @RequirePermission("system:dict:list")
    @GetMapping("/type/{dictType}")
    public Result<List<SysDictData>> getByType(@PathVariable String dictType) {
        List<SysDictData> list = dictDataService.selectDictDataByType(dictType);
        return Result.success(list);
    }
}