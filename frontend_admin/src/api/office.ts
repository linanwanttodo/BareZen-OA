import request from './request'
import type { PageData } from '@/types/api'

// ============ 会议室 ============
export interface OfficeMeetingRoom {
  id: string
  roomName: string
  capacity: number
  location: string
  equipment: string
  status: string
  remark: string
  createTime: string
}

export interface RoomListParams {
  pageNum?: number
  pageSize?: number
  roomName?: string
  status?: string
}

/** 会议室分页列表 */
export function getRoomPageApi(params: RoomListParams) {
  return request.get<PageData<OfficeMeetingRoom>>('/office/v1/rooms/page', { params })
}

/** 可用会议室列表 */
export function getAvailableRoomsApi() {
  return request.get<OfficeMeetingRoom[]>('/office/v1/rooms/available')
}

/** 新增会议室 */
export function addRoomApi(data: Partial<OfficeMeetingRoom>) {
  return request.post<void>('/office/v1/rooms', data)
}

/** 更新会议室 */
export function updateRoomApi(data: Partial<OfficeMeetingRoom>) {
  return request.put<void>('/office/v1/rooms', data)
}

/** 删除会议室 */
export function deleteRoomApi(id: string) {
  return request.delete<void>(`/office/v1/rooms/${id}`)
}

// ============ 会议预订 ============
export interface OfficeMeetingBooking {
  id: string
  roomId: string
  roomName: string
  title: string
  bookerId: string
  bookerName: string
  startTime: string
  endTime: string
  attendees: string
  remark: string
  createTime: string
}

export interface BookingListParams {
  pageNum?: number
  pageSize?: number
  title?: string
  roomId?: string
}

/** 会议预订分页列表 */
export function getBookingPageApi(params: BookingListParams) {
  return request.get<PageData<OfficeMeetingBooking>>('/office/v1/bookings/page', { params })
}

/** 新增会议预订 */
export function addBookingApi(data: Partial<OfficeMeetingBooking>) {
  return request.post<void>('/office/v1/bookings', data)
}

/** 编辑会议预订 */
export function updateBookingApi(data: Partial<OfficeMeetingBooking>) {
  return request.put<void>(`/office/v1/bookings/${data.id}`, data)
}

/** 删除会议预订 */
export function deleteBookingApi(id: string) {
  return request.delete<void>(`/office/v1/bookings/${id}`)
}

// ============ 用车申请 ============
export interface OfficeVehicleRequest {
  id: string
  applicantId: string
  applicantName: string
  deptName: string
  vehicleType: string
  purpose: string
  startTime: string
  endTime: string
  destination: string
  passengerCount: number
  status: string
  approverId: string
  approverName: string
  approveTime: string
  approveRemark: string
  createTime: string
}

export interface VehicleListParams {
  pageNum?: number
  pageSize?: number
  status?: string
  applicantName?: string
}

/** 用车申请分页列表 */
export function getVehiclePageApi(params: VehicleListParams) {
  return request.get<PageData<OfficeVehicleRequest>>('/office/v1/vehicles/page', { params })
}

/** 提交用车申请 */
export function submitVehicleApi(data: Partial<OfficeVehicleRequest>) {
  return request.post<void>('/office/v1/vehicles', data)
}

/** 修改用车申请（仅本人、仅待审批状态） */
export function updateVehicleApi(id: string, data: Partial<OfficeVehicleRequest>) {
  return request.put<void>(`/office/v1/vehicles/${id}`, data)
}

/** 审批用车申请 */
export function approveVehicleApi(id: string, action: 'pass' | 'reject', remark?: string) {
  return request.put<void>(`/office/v1/vehicles/${id}/approve`, null, {
    params: { action, remark },
  })
}

/** 取消用车申请 */
export function cancelVehicleApi(id: string) {
  return request.put<void>(`/office/v1/vehicles/${id}/cancel`)
}

/** 删除用车申请 */
export function deleteVehicleApi(id: string) {
  return request.delete<void>(`/office/v1/vehicles/${id}`)
}
