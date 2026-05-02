-- ============================================
-- BareZen_Base V21: 日程提醒支持
-- ============================================
-- 定时任务到点推送日程提醒，需标记已提醒避免重复推送。

ALTER TABLE `hr_schedule`
  ADD COLUMN `remind_sent` char(1) DEFAULT 'N' COMMENT '是否已提醒（Y=是 N=否）' AFTER `location`;
