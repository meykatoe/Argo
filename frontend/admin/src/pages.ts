import type { Component } from 'vue'
import CardTable from './components/CardTable.vue'

// 選單路徑對應的頁面，新頁面在此登記
export const pages: Record<string, Component> = {
  '/cards': CardTable,
}
