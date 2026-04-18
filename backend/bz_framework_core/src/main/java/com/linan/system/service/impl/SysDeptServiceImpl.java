package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysDept;
import com.linan.system.mapper.SysDeptMapper;
import com.linan.system.service.ISysDeptService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门 Service 实现
 *
 * @author BareZen Team
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {

    @Override
    public List<SysDept> getDeptTree() {
        // 查出所有未删除部门，前端递归构建树
        return baseMapper.selectAllDept();
    }
}
