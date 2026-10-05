import request from './request'

// ========== 审核服务 AuditController /api/v1/audit ==========

// 待审核列表
export function getPendingAuditsApi(params = {}) {
  return request.get('/audit/pending', { params })
}

// 审核记录列表
export function getAuditRecordsApi(params = {}) {
  return request.get('/audit/records', { params })
}

// 审核记录详情
export function getAuditRecordApi(recordId) {
  return request.get(`/audit/records/${recordId}`)
}

// 审核处理（通过/拒绝/下架）
export function processAuditApi(recordId, payload) {
  return request.post(`/audit/records/${recordId}/process`, payload)
}

// 操作日志查询
export function getAuditLogsApi(params = {}) {
  return request.get('/audit/logs', { params })
}
