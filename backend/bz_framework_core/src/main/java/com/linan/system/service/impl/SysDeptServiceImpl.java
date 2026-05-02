package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Override
    public boolean hasChildDept(Long deptId) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getParentId, deptId)
                        .eq(SysDept::getDelFlag, 0));
        return count > 0;
    }

    @Override
    public void updateDeptStatus(Long deptId, String status) {
        SysDept dept = new SysDept();
        dept.setId(deptId);
        dept.setStatus(status);
        baseMapper.updateById(dept);
    }
}
