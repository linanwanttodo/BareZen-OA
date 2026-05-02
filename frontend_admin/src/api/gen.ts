import request from './request'

export interface TableInfo {
  tableName: string
  tableComment: string
  className: string
  classVar: string
  columns: ColumnInfo[]
}

export interface ColumnInfo {
  columnName: string
  columnComment: string
  javaType: string
  javaField: string
  pk: boolean
  required: boolean
  insert: boolean
  edit: boolean
  list: boolean
  query: boolean
  queryType: string
  htmlType: string
  dictType: string
}

/** 查询数据库表列表 */
export function getGenTableListApi(tableName?: string) {
  return request.get<TableInfo[]>('/system/v1/gen/tables', { params: { tableName } })
}

/** 获取表详细信息 */
export function getGenTableDetailApi(tableName: string) {
  return request.get<TableInfo>(`/system/v1/gen/table/${encodeURIComponent(tableName)}`)
}

/** 生成代码 */
export function generateCodeApi(tableName: string) {
  return request.post<Record<string, string>>(`/system/v1/gen/gen/${encodeURIComponent(tableName)}`)
}
