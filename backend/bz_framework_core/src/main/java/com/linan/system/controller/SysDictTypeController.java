package com.linan.system.controller;

import com.linan.core.common.controller.BaseController;
import com.linan.system.entity.SysDictType;
import com.linan.system.service.ISysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/v1/dict/types")
public class SysDictTypeController extends BaseController<SysDictType, ISysDictTypeService> {
    @Autowired private ISysDictTypeService dictTypeService;
    @Override protected ISysDictTypeService getService() { return dictTypeService; }
}