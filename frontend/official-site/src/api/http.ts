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
      query.set(key, String(value))
    }
  }
  // 依目前語言取資料
  query.set('lang', currentLocale())
  const res = await fetch(`${BASE}${path}?${query.toString()}`, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!res.ok) {
    throw await readError(res)
  }
  return (await res.json()) as T
}

export function get<T>(path: string, params: Params = {}): Promise<T> {
  return request<T>('GET', path, params)
}

export function post<T>(path: string, body: unknown, params: Params = {}): Promise<T> {
  return request<T>('POST', path, params, body)
}

async function readError(res: Response): Promise<ApiError> {
  try {
    const body = await res.json()
    return new ApiError(res.status, body.code ?? 'ERROR', body.details ?? {})
  } catch {
    return new ApiError(res.status, 'ERROR')
  }
}
