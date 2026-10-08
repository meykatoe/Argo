import { vi } from 'vitest'
import type { StaffConfig } from '../config'
import { createErrorTexts } from '../utils/error'

// 測試用設定，api 全部是假的
export function fakeConfig(over: Partial<StaffConfig['api']> = {}, pages: StaffConfig['pages'] = {}): StaffConfig {
  return {
    storageKey: 'test-session',
    brand: '測試後台',
    title: 'Argo 測試',
    pages,
    errors: createErrorTexts(),
    api: {
      login: vi.fn(),
      logout: vi.fn().mockResolvedValue(null),
      getMenu: vi.fn().mockResolvedValue([]),
      ...over,
    },
  }
}
