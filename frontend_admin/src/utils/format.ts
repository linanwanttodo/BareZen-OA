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

export function statusText(status: string | undefined | null): string {
  if (status === '0') return '正常';
  if (status === '1') return '停用';
  return '-';
}

export function statusBadge(status: string | undefined | null): string {
  if (status === '0') return 'success';
  if (status === '1') return 'error';
  return 'default';
}

export function noticeStatusText(status: string | undefined | null): string {
  if (status === '0') return '草稿';
  if (status === '1') return '已发布';
  if (status === '2') return '已下架';
  return '-';
}

export function businessTypeText(type: number | undefined | null): string {
  const map: Record<number, string> = { 0: '其它', 1: '新增', 2: '修改', 3: '删除', 4: '查询', 5: '登录', 6: '导出' };
  return type != null ? map[type] || '其它' : '-';
}

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline'

export function operStatusText(status: number | undefined | null): string {
  if (status === 0) return '成功';
  if (status === 1) return '失败';
  return '-';
}

export function operStatusVariant(status: number | undefined | null): BadgeVariant {
  if (status === 0) return 'default';
  if (status === 1) return 'destructive';
  return 'outline';
}

export function businessTypeVariant(type: number | undefined | null): BadgeVariant {
  const map: Record<number, BadgeVariant> = { 0: 'outline', 1: 'default', 2: 'secondary', 3: 'destructive' };
  return type != null ? map[type] ?? 'outline' : 'outline';
}

export function methodVariant(method: string | undefined | null): BadgeVariant {
  const map: Record<string, BadgeVariant> = { GET: 'outline', POST: 'default', PUT: 'secondary', DELETE: 'destructive', PATCH: 'outline' };
  return map[method?.toUpperCase() ?? ''] ?? 'outline';
}

export function menuTypeText(type: string | undefined | null): string {
  if (type === 'M') return '目录';
  if (type === 'C') return '菜单';
  if (type === 'F') return '按钮';
  return '-';
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
