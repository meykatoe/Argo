import { createErrorTexts } from '@shared/utils/error'

export const { errorText, lockedMessage } = createErrorTexts({
  CARD_NOT_FOUND: '找不到這張卡片',
  VALIDATION_ERROR: '折扣必須大於 0 且不超過 1，最多四位小數',
  SET_NOT_FOUND: '找不到這個系列',
  ORDER_NOT_FOUND: '找不到這筆訂單',
  ORDER_STATE_CONFLICT: '訂單狀態已變更，請重新整理後再試',
})
