package com.linan.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.common.exception.BusinessException;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.mapper.AttLeaveRequestMapper;
import com.linan.hr.service.IAttLeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 请假申请服务实现
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class AttLeaveRequestServiceImpl extends ServiceImpl<AttLeaveRequestMapper, AttLeaveRequest>
        implements IAttLeaveRequestService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLeaveRequest(AttLeaveRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (request.getStartTime() == null || request.getStartTime().isBlank()) {
            throw new BusinessException("请选择请假开始时间");
        }
        if (request.getEndTime() == null || request.getEndTime().isBlank()) {
            throw new BusinessException("请选择请假结束时间");
        }

        // 验证时间合法性
        if (!validateLeaveTime(request.getStartTime(), request.getEndTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        // 设置默认值
        if (request.getStatus() == null) {
            request.setStatus(ApprovalStatus.PENDING.getValue());
        }
        if (request.getUserName() == null || request.getUserName().isBlank()) {
            request.setUserName("用户" + request.getUserId());
        }

        boolean ok = save(request);
        if (!ok) {
            throw new BusinessException("提交请假申请失败");
        }
    }

    @Override
    public List<AttLeaveRequest> listByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<AttLeaveRequest>()
                .eq(AttLeaveRequest::getUserId, userId)
                .orderByDesc(AttLeaveRequest::getCreateTime));
    }

    @Override
    public boolean validateLeaveTime(String startTime, String endTime) {
        if (startTime == null || startTime.isBlank() || endTime == null || endTime.isBlank()) {
            return false;
        }
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, FMT);
            LocalDateTime end = LocalDateTime.parse(endTime, FMT);
            return start.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }
}
