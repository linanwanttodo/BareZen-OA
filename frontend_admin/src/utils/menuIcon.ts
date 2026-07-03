import type { Component } from 'vue'
import {
  AppIcon,
  BackupIcon,
  BookIcon,
  CalendarIcon,
  ChartBarIcon,
  CloudIcon,
  CodeIcon,
  ControlPlatformIcon,
  DashboardIcon,
  DesktopIcon,
  EditIcon,
  FileCopyIcon,
  FileIcon,
  FolderIcon,
  LinkIcon,
  LockOnIcon,
  LoginIcon,
  MoneyIcon,
  NotificationIcon,
  SendIcon,
  SettingIcon,
  TableIcon,
  TagIcon,
  TimeIcon,
  ToolsIcon,
  TreeListIcon,
  UploadIcon,
  UsergroupIcon,
  UserIcon,
  ViewListIcon,
} from 'tdesign-icons-vue-next'

/**
 * sys_menu.icon → TDesign 图标组件。
 *
 * 库里的 icon 值沿用自早期模板的 Ant Design 图标命名（bar-chart / money-circle /
 * logininfor 等），不是 TDesign 图标名，不能直接 <component :is> 拼接使用。
 * 这里显式建表并提供兜底，保证侧边菜单图标可控，未登记的取值退回 FolderIcon
 * 而不是解析失败留下空白槽位。
 */
const MENU_ICONS: Record<string, Component> = {
  apps: AppIcon,
  appstore: AppIcon,
  backup: BackupIcon,
  'bar-chart': ChartBarIcon,
  book: BookIcon,
  calendar: CalendarIcon,
  clock: TimeIcon,
  code: CodeIcon,
  dashboard: DashboardIcon,
  desktop: DesktopIcon,
  drive: CloudIcon,
  edit: EditIcon,
  file: FileIcon,
  'file-text': FileCopyIcon,
  flight: SendIcon,
  folder: FolderIcon,
  job: TimeIcon,
  link: LinkIcon,
  list: ViewListIcon,
  logininfor: LoginIcon,
  'money-circle': MoneyIcon,
  monitor: ControlPlatformIcon,
  notification: NotificationIcon,
  peoples: UsergroupIcon,
  plugin: ToolsIcon,
  safe: LockOnIcon,
  setting: SettingIcon,
  storage: CloudIcon,
  system: SettingIcon,
  tags: TagIcon,
  tool: ToolsIcon,
  tree: TreeListIcon,
  'tree-table': TableIcon,
  upload: UploadIcon,
  user: UserIcon,
}

export function resolveMenuIcon(icon?: string): Component {
  if (!icon || icon === '#') return FolderIcon
  return MENU_ICONS[icon] ?? FolderIcon
}
