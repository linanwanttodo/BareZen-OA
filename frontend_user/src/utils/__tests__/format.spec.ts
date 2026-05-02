import { describe, it, expect } from 'vitest'
import {
  formatDate,
  formatDateTime,
  formatDateTimeFull,
  noticeTypeText,
  priorityText,
  todoStatusText,
  formatShortDate,
} from '@/utils/format'

describe('format 工具函数', () => {
  it('formatDate 截取前10位日期', () => {
    expect(formatDate('2026-08-02 10:30:00')).toBe('2026-08-02')
    expect(formatDate('2026-08-02')).toBe('2026-08-02')
    expect(formatDate(null)).toBe('-')
    expect(formatDate('')).toBe('-')
  })

  it('formatDateTime 截取前16位', () => {
    expect(formatDateTime('2026-08-02 10:30:00')).toBe('2026-08-02 10:30')
    expect(formatDateTime(undefined)).toBe('-')
  })

  it('formatDateTimeFull 截取前19位', () => {
    expect(formatDateTimeFull('2026-08-02 10:30:00')).toBe('2026-08-02 10:30:00')
    expect(formatDateTimeFull(undefined)).toBe('-')
  })

  it('formatShortDate 截取月日', () => {
    expect(formatShortDate('2026-08-02 10:30:00')).toBe('08-02')
    expect(formatShortDate(null)).toBe('-')
  })

  it('noticeTypeText 映射类型', () => {
    expect(noticeTypeText('1')).toBe('通知')
    expect(noticeTypeText('2')).toBe('公告')
    expect(noticeTypeText('x')).toBe('-')
    expect(noticeTypeText(null)).toBe('-')
  })

  it('priorityText 映射优先级', () => {
    expect(priorityText(0)).toBe('低')
    expect(priorityText(1)).toBe('中')
    expect(priorityText(2)).toBe('高')
    expect(priorityText(3)).toBe('紧急')
    expect(priorityText(99)).toBe('中')
    expect(priorityText(null)).toBe('-')
  })

  it('todoStatusText 映射待办状态', () => {
    expect(todoStatusText(0)).toBe('待办')
    expect(todoStatusText(1)).toBe('进行中')
    expect(todoStatusText(2)).toBe('已完成')
    expect(todoStatusText(3)).toBe('已取消')
    expect(todoStatusText(99)).toBe('待办')
    expect(todoStatusText(null)).toBe('-')
  })
})
