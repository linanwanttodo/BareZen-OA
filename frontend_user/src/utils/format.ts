export function formatDate(dateStr: string | undefined | null): string {
  if (!dateStr) return '-';
  return dateStr.substring(0, 10);
}

export function formatDateTime(dateStr: string | undefined | null): string {
  if (!dateStr) return '-';
  return dateStr.substring(0, 16);
}

export function formatDateTimeFull(dateStr: string | undefined | null): string {
  if (!dateStr) return '-';
  return dateStr.substring(0, 19);
}

export function noticeTypeText(type: string | undefined | null): string {
  if (type === '1') return '通知';
  if (type === '2') return '公告';
  return '-';
}

export function priorityText(priority: number | undefined | null): string {
  const map: Record<number, string> = { 0: '低', 1: '中', 2: '高', 3: '紧急' };
  return priority != null ? map[priority] || '中' : '-';
}

export function todoStatusText(status: number | undefined | null): string {
  const map: Record<number, string> = { 0: '待办', 1: '进行中', 2: '已完成', 3: '已取消' };
  return status != null ? map[status] || '待办' : '-';
}

export function formatShortDate(dateStr: string | undefined | null): string {
  if (!dateStr) return '-';
  return dateStr.substring(5, 10);
}
