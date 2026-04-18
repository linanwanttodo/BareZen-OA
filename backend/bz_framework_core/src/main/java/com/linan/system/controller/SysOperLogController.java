package com.linan.system.controller;

import com.linan.core.common.controller.BaseController;
import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/v1/operlogs")
public class SysOperLogController extends BaseController<SysOperLog, ISysOperLogService> {
    @Autowired private ISysOperLogService operLogService;
    @Override protected ISysOperLogService getService() { return operLogService; }
}