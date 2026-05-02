package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 部门 Mapper 接口
 *
 * @author BareZen Team
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询所有未删除的部门，用于构建树
     */
    @Select("SELECT * FROM sys_dept WHERE del_flag = 0 ORDER BY sort ASC")
    List<SysDept> selectAllDept();

    /**
     * 根据父部门ID查询所有子部门ID（用于数据权限递归）
     */
    @Select("SELECT id FROM sys_dept WHERE parent_id = #{parentId} AND del_flag = 0")
    List<Long> selectDeptIdsByParentId(@Param("parentId") Long parentId);
}
