const LABELS: Record<string, string> = {
  ADMIN: '最高權限',
  GENERAL: '一般管理員',
  SERVICE: '客服',
  OPS: '運維',
}

export function roleName(role: string): string {
  return LABELS[role] ?? role
}
