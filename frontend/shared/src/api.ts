const BASE = import.meta.env.VITE_API_BASE ?? '/api'

// 後端統一回傳格式
export interface Result<T> {
  code: number
  msg: string
  data: T
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: Record<string, string>

  constructor(status: number, code: string, details: Record<string, string> = {}) {
    super(code)
    this.status = status
    this.code = code
    this.details = details
  }
}

export type Params = Record<string, string | number | boolean | undefined>

export async function request<T>(
  token: string | null,
  method: 'GET' | 'POST' | 'PATCH' | 'DELETE',
  path: string,
  params: Params = {},
  body?: unknown,
): Promise<T> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    // 略過空值參數
    if (value !== undefined && value !== '') {
      // 是否類參數一律送 1 或 0
      query.set(key, typeof value === 'boolean' ? (value ? '1' : '0') : String(value))
    }
  }
  const headers: Record<string, string> = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const res = await fetch(`${BASE}${path}?${query.toString()}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  let result: Result<T> | null = null
  try {
    const body = await res.json()
    result = body && typeof body === 'object' && typeof body.code === 'number' ? body : null
  } catch {
    // 非 JSON 回應
  }
  // 成功代碼固定為 200，失敗時 msg 為錯誤代碼
  if (!res.ok || result?.code !== 200) {
    const d = result?.data
    throw new ApiError(
      res.status,
      result?.msg ?? 'ERROR',
      d && typeof d === 'object' ? (d as Record<string, string>) : {},
    )
  }
  return result.data
}
