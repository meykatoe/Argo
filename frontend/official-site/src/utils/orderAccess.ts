const KEY = 'argo.orderAccess'

type AccessMap = Record<string, string>

function read(): AccessMap {
  try {
    const raw = JSON.parse(sessionStorage.getItem(KEY) ?? '{}')
    return raw && typeof raw === 'object' ? raw : {}
  } catch {
    return {}
  }
}

// 只存在分頁期間，關閉分頁就消失
export function saveOrderEmail(orderNo: string, email: string) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({ ...read(), [orderNo]: email }))
  } catch {
    // 無法儲存就每次手動輸入
  }
}

export function getOrderEmail(orderNo: string): string {
  return read()[orderNo] ?? ''
}

export function forgetOrderEmail(orderNo: string) {
  const map = read()
  delete map[orderNo]
  try {
    sessionStorage.setItem(KEY, JSON.stringify(map))
  } catch {
    // 忽略
  }
}
