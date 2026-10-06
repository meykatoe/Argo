import { currentLocale } from '@/i18n'

const BASE = import.meta.env.VITE_API_BASE ?? '/api'

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

type Params = Record<string, string | number | boolean | undefined>

async function request<T>(
  method: 'GET' | 'POST',
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
  // 依目前語言取資料
  query.set('lang', currentLocale())
  const res = await fetch(`${BASE}${path}?${query.toString()}`, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const result = await readResult<T>(res)
  // 成功代碼固定為 200
  if (!res.ok || result?.code !== 200) {
    throw new ApiError(res.status, result?.msg ?? 'ERROR', toDetails(result?.data))
  }
  return result.data
}

export function get<T>(path: string, params: Params = {}): Promise<T> {
  return request<T>('GET', path, params)
}

export function post<T>(path: string, body: unknown, params: Params = {}): Promise<T> {
  return request<T>('POST', path, params, body)
}

// 後端統一回傳格式，失敗時 msg 為錯誤代碼、data 為欄位細節
export interface Result<T> {
  code: number
  msg: string
  data: T
}

async function readResult<T>(res: Response): Promise<Result<T> | null> {
  try {
    const body = await res.json()
    return body && typeof body === 'object' && typeof body.code === 'number' ? body : null
  } catch {
    return null
  }
}

function toDetails(data: unknown): Record<string, string> {
  return data && typeof data === 'object' ? (data as Record<string, string>) : {}
}
