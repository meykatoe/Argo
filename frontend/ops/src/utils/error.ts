import { createErrorTexts } from '@shared/utils/error'

export const { errorText, lockedMessage } = createErrorTexts({
  PROTECTED_IP: '這個位址不可封鎖（本機、代理或系統保留位址）',
  CANNOT_BLOCK_SELF: '不能封鎖你自己目前使用的位址',
  INVALID_IP: 'IP 格式不正確',
  IP_NOT_BLOCKED: '這個 IP 目前沒有被封鎖',
  INVALID_RANGE: '開始時間必須早於結束時間',
  ORDER_STATE_CONFLICT: '訂單狀態已變更，請重新整理後再試',
  MENU_NOT_FOUND: '找不到這個選單，請重新整理後再試',
  MENU_NOT_GRANTABLE: '群組不能授權，請授權底下的功能',
  ROLE_PORTAL_MISMATCH: '這個角色不屬於該後台入口',
  PERMISSION_PROTECTED: '這是權限管理本身，不能停用或撤銷運維，否則會鎖死自己',
  BAD_REQUEST: '查詢條件有誤',
})
