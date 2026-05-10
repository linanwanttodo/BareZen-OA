package com.linan.hr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.hr.entity.AttLeaveRequest;

import java.util.List;

/**
 * 请假申请服务接口
 *
 * @author BareZen Team
 */
public interface IAttLeaveRequestService extends IService<AttLeaveRequest> {

    /**
     * 提交请假申请
     *
     * @param request 请假申请信息
     * @throws IllegalArgumentException 参数不合法
     */
    void submitLeaveRequest(AttLeaveRequest request);

    /**
     * 查询用户的请假记录
     *
     * @param userId 用户ID
     * @return 请假记录列表
     */
    List<AttLeaveRequest> listByUserId(Long userId);

    /**
     * 检查请假时间是否合法
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return true=合法，false=不合法
     */
    boolean validateLeaveTime(String startTime, String endTime);
}
