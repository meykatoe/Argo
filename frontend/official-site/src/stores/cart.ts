import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

export interface CartItem {
  id: number
  qty: number
}

export const MAX_LINE_QTY = 99
const STORAGE_KEY = 'argo.cart'

// 讀取時略過不合法的資料
function load(): CartItem[] {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
    if (!Array.isArray(raw)) return []
    return raw
      .filter((i) => Number.isInteger(i?.id) && Number.isInteger(i?.qty) && i.qty > 0)
      .map((i) => ({ id: i.id, qty: Math.min(i.qty, MAX_LINE_QTY) }))
  } catch {
    return []
  }
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>(load())

  const totalQty = computed(() => items.value.reduce((sum, i) => sum + i.qty, 0))

  function qtyOf(id: number): number {
    return items.value.find((i) => i.id === id)?.qty ?? 0
  }

  function add(id: number, qty = 1) {
    setQty(id, qtyOf(id) + qty)
  }

  // 數量歸零等於移除
  function setQty(id: number, qty: number) {
    const next = Math.min(Math.floor(qty), MAX_LINE_QTY)
    const line = items.value.find((i) => i.id === id)
    if (!(next > 0)) {
      remove(id)
    } else if (line) {
      line.qty = next
    } else {
      items.value.push({ id, qty: next })
    }
  }

  function remove(id: number) {
    items.value = items.value.filter((i) => i.id !== id)
  }

  function clear() {
    items.value = []
  }

  watch(
    items,
    (value) => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
      } catch {
        // 儲存失敗不影響使用
      }
    },
    { deep: true },
  )

  return { items, totalQty, qtyOf, add, setQty, remove, clear }
})
