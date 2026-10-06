import type { Component } from 'vue'
import AuditTable from './components/AuditTable.vue'

// 選單路徑對應的頁面，新頁面在此登記
export const pages: Record<string, Component> = {
  '/audit-logs': AuditTable,
}
