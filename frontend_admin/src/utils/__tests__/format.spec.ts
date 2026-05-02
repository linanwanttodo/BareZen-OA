import { describe, it, expect } from 'vitest'
import {
  formatDate,
  formatDateTime,
  formatDateTimeFull,
  statusText,
  statusBadge,
  noticeStatusText,
} from '@/utils/format'

describe('format 工具函数', () => {
  it('formatDate 截取前10位日期', () => {
    expect(formatDate('2026-08-02 10:30:00')).toBe('2026-08-02')
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

  it('statusText 映射状态', () => {
    expect(statusText('0')).toBe('正常')
    expect(statusText('1')).toBe('停用')
    expect(statusText('x')).toBe('-')
    expect(statusText(null)).toBe('-')
  })

  it('statusBadge 映射徽章', () => {
    expect(statusBadge('0')).toBe('success')
    expect(statusBadge('1')).toBe('error')
    expect(statusBadge('x')).toBe('default')
  })

  it('noticeStatusText 映射公告状态', () => {
    expect(noticeStatusText('0')).toBe('草稿')
    expect(noticeStatusText('1')).toBe('已发布')
  })
})
