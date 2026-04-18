package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysDept;

import java.util.List;

/**
 * 部门 Service 接口
 *
 * @author BareZen Team
 */
public interface ISysDeptService extends IService<SysDept> {

    /**
     * 获取部门树形列表
     */
    List<SysDept> getDeptTree();
}
