import { currentLocale } from '@/i18n'

const BASE = import.meta.env.VITE_API_BASE ?? '/api'

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string) {
    super(code)
    this.status = status
    this.code = code
  }
}

type Params = Record<string, string | number | boolean | undefined>

export async function get<T>(path: string, params: Params = {}): Promise<T> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    // 略過空值參數
    if (value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  }
  // 依目前語言取資料
  query.set('lang', currentLocale())
  const res = await fetch(`${BASE}${path}?${query.toString()}`)
  if (!res.ok) {
    throw new ApiError(res.status, await readCode(res))
  }
  return (await res.json()) as T
}

async function readCode(res: Response): Promise<string> {
  try {
    const body = await res.json()
    return body.code ?? 'ERROR'
  } catch {
    return 'ERROR'
  }
}
