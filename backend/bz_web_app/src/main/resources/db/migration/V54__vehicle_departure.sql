-- ============================================
-- BareZen_Base V54: 用车申请表新增出发地字段
-- ============================================
-- 背景：用户前台用车申请表单需要填写出发地（车型/事由/出发地/目的地/起止时间），
--       原 office_vehicle_request 表只有目的地（destination），补 departure 列。
--       管理端已有存量数据不受影响，departure 允许为空。

ALTER TABLE `office_vehicle_request`
  ADD COLUMN `departure` VARCHAR(200) DEFAULT NULL COMMENT '出发地' AFTER `vehicle_type`;
