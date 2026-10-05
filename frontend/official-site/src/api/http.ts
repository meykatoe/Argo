const BASE = import.meta.env.VITE_API_BASE ?? '/api'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
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
  const qs = query.toString()
  const res = await fetch(`${BASE}${path}${qs ? `?${qs}` : ''}`)
  if (!res.ok) {
    throw new ApiError(res.status, await readMessage(res))
  }
  return (await res.json()) as T
}

async function readMessage(res: Response): Promise<string> {
  try {
    const body = await res.json()
    return body.message ?? body.error ?? res.statusText
  } catch {
    return res.statusText
  }
}
