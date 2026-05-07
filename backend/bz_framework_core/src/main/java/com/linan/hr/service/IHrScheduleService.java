package com.linan.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.hr.entity.HrSchedule;

/**
 * 个人日程服务接口
 */
public interface IHrScheduleService extends IService<HrSchedule> {

    /**
     * 分页查询全员日程，并按 userId 回填昵称到 userName 字段（便于前端展示）。
     *
     * @param page    分页参数
     * @param wrapper 查询条件（由 Controller 组装）
     * @return 已回填昵称的分页结果
     */
    IPage<HrSchedule> pageWithUserName(Page<HrSchedule> page, LambdaQueryWrapper<HrSchedule> wrapper);

    /**
     * 新增日程：userName 缺省时按 userName 关联 sys_user 回填 userId；
     * userName 也缺省时回退到当前登录用户。
     *
     * @param schedule 日程实体
     * @return 是否成功
     */
    boolean resolveAndCreate(HrSchedule schedule);
}