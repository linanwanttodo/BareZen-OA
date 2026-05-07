package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.mapper.HrScheduleMapper;
import com.linan.hr.service.IHrScheduleService;
import com.linan.system.entity.SysUser;
import com.linan.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 个人日程服务实现
 *
 * <p>承载 userId→昵称回显、userName→userId 回填等展示/归属解析，Controller 仅组装查询条件。</p>
 */
@Service
@RequiredArgsConstructor
public class HrScheduleServiceImpl extends ServiceImpl<HrScheduleMapper, HrSchedule>
        implements IHrScheduleService {

    private final ISysUserService userService;

    @Override
    public IPage<HrSchedule> pageWithUserName(Page<HrSchedule> page, LambdaQueryWrapper<HrSchedule> wrapper) {
        IPage<HrSchedule> result = page(page, wrapper);
        List<HrSchedule> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> userIds = records.stream()
                    .map(HrSchedule::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                Map<Long, SysUser> userMap = userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
                records.forEach(s -> {
                    SysUser u = userMap.get(s.getUserId());
                    if (u != null) {
                        s.setUserName(u.getNickName() != null ? u.getNickName() : u.getUserName());
                    }
                });
            }
        }
        return result;
    }

    @Override
    public boolean resolveAndCreate(HrSchedule schedule) {
        if (schedule.getUserId() == null) {
            Long userId = null;
            if (schedule.getUserName() != null && !schedule.getUserName().isEmpty()) {
                List<SysUser> users = userService.list(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUserName, schedule.getUserName())
                        .or().eq(SysUser::getNickName, schedule.getUserName())
                        .last("LIMIT 1"));
                if (!users.isEmpty()) {
                    userId = users.get(0).getId();
                }
            }
            if (userId == null) {
                userId = AuthInterceptor.getCurrentUserId();
            }
            schedule.setUserId(userId);
        }
        return save(schedule);
    }
}