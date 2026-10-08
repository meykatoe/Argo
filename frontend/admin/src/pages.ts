import type { Component } from 'vue'
import CardSetTable from './components/CardSetTable.vue'
import CardTable from './components/CardTable.vue'
import OrderTable from './components/OrderTable.vue'

// 選單路徑對應的頁面，新頁面在此登記
export const pages: Record<string, Component> = {
  '/series': CardSetTable,
  '/cards': CardTable,
  '/orders': OrderTable,
}
