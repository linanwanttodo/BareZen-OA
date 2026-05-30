package com.linan.office.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeMeetingBooking;
import com.linan.office.entity.OfficeMeetingRoom;
import com.linan.office.mapper.OfficeMeetingBookingMapper;
import com.linan.office.mapper.OfficeMeetingRoomMapper;
import com.linan.office.service.IOfficeMeetingBookingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会议预订服务实现
 *
 * @author BareZen Team
 */
@Service
public class OfficeMeetingBookingServiceImpl extends ServiceImpl<OfficeMeetingBookingMapper, OfficeMeetingBooking>
        implements IOfficeMeetingBookingService {

    private final OfficeMeetingRoomMapper roomMapper;

    public OfficeMeetingBookingServiceImpl(OfficeMeetingRoomMapper roomMapper) {
        this.roomMapper = roomMapper;
    }

    /**
     * 新增会议预订（事务内行锁 + 时间冲突校验，保证并发安全）
     *
     * <p>业务失败抛出 {@link BusinessException}，由全局异常处理器统一返回 Result，
     * 事务自动回滚，避免「行锁已获取但保存失败」留下脏数据。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBooking(OfficeMeetingBooking booking) {
        // 悲观锁：锁定会议室行，使同一会议室的并发预订串行化
        // （仅当锁查询结果为空时回退到普通查询，兼容无 FOR UPDATE 权限的只读场景）
        OfficeMeetingRoom room = roomMapper.selectByIdForUpdate(booking.getRoomId());
        if (room == null) {
            throw new BusinessException("会议室不存在");
        }
        booking.setRoomName(room.getRoomName());

        // 时间冲突校验：同一会议室时间区间重叠则拒绝
        long conflict = lambdaQuery()
                .eq(OfficeMeetingBooking::getRoomId, booking.getRoomId())
                .and(w -> w
                        .lt(OfficeMeetingBooking::getStartTime, booking.getEndTime())
                        .gt(OfficeMeetingBooking::getEndTime, booking.getStartTime()))
                .count();
        if (conflict > 0) {
            throw new BusinessException("该会议室在所选时间段已被预订");
        }

        // 预订人自动填充：后端兜底填真实用户名（JWT 中解析），禁止把 userId 当姓名落库
        Long userId = AuthInterceptor.getCurrentUserId();
        booking.setBookerId(userId);
        if (booking.getBookerName() == null || booking.getBookerName().isEmpty()) {
            booking.setBookerName(userId != null ? AuthInterceptor.getCurrentUsername() : null);
        }
        if (!save(booking)) {
            throw new BusinessException("预订失败");
        }
    }

    /**
     * 编辑会议预订（复用新增路径的冲突校验与行锁机制，排除自身 id）
     *
     * <p>限制可更新字段白名单：title/startTime/endTime/attendees/remark；
     * 强制回填 roomId/bookerName 以原始记录为准。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBooking(Long id, OfficeMeetingBooking booking) {
        OfficeMeetingBooking existing = getById(id);
        if (existing == null) {
            throw new BusinessException("预订不存在");
        }

        // 白名单字段拷贝：仅允许更新 title/startTime/endTime/attendees/remark
        existing.setTitle(booking.getTitle());
        existing.setStartTime(booking.getStartTime());
        existing.setEndTime(booking.getEndTime());
        existing.setAttendees(booking.getAttendees());
        existing.setRemark(booking.getRemark());

        // 强制保留原始归属信息，防止篡改
        existing.setRoomId(existing.getRoomId());
        existing.setRoomName(existing.getRoomName());
        existing.setBookerId(existing.getBookerId());
        existing.setBookerName(existing.getBookerName());

        // 时间冲突校验：排除自身 id
        long conflict = lambdaQuery()
                .eq(OfficeMeetingBooking::getRoomId, existing.getRoomId())
                .ne(OfficeMeetingBooking::getId, id)
                .and(w -> w
                        .lt(OfficeMeetingBooking::getStartTime, existing.getEndTime())
                        .gt(OfficeMeetingBooking::getEndTime, existing.getStartTime()))
                .count();
        if (conflict > 0) {
            throw new BusinessException("该会议室在所选时间段已被预订");
        }

        if (!updateById(existing)) {
            throw new BusinessException("编辑失败");
        }
    }
}
