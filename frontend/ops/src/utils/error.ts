import { createErrorTexts } from '@shared/utils/error'

export const { errorText, lockedMessage } = createErrorTexts({
  PROTECTED_IP: '這個位址不可封鎖（本機、代理或系統保留位址）',
  CANNOT_BLOCK_SELF: '不能封鎖你自己目前使用的位址',
  INVALID_IP: 'IP 格式不正確',
  IP_NOT_BLOCKED: '這個 IP 目前沒有被封鎖',
  INVALID_RANGE: '開始時間必須早於結束時間',
  ORDER_STATE_CONFLICT: '訂單狀態已變更，請重新整理後再試',
  BAD_REQUEST: '查詢條件有誤',
})
