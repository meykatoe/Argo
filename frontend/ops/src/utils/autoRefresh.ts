import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const KEY = 'argo.ops.autoRefresh'
export const REFRESH_MS = 10_000

function read(): boolean {
  try {
    // 預設開啟
    return localStorage.getItem(KEY) !== '0'
  } catch {
    return true
  }
}

// 定時重新載入：分頁在背景、正在載入、或頁面說現在不適合時都會略過
export function useAutoRefresh(tick: () => Promise<void> | void, canRun: () => boolean = () => true, ms = REFRESH_MS) {
  const enabled = ref(read())
  let timer: ReturnType<typeof setInterval> | undefined
  let busy = false

  async function run() {
    if (!enabled.value || busy || document.hidden || !canRun()) return
    busy = true
    try {
      await tick()
    } finally {
      busy = false
    }
  }

  // 回到這個分頁就立刻更新一次
  function onVisible() {
    if (!document.hidden) run()
  }

  watch(enabled, (v) => {
    try {
      localStorage.setItem(KEY, v ? '1' : '0')
    } catch {
      // 無法儲存就只在本次有效
    }
  })

  onMounted(() => {
    timer = setInterval(run, ms)
    document.addEventListener('visibilitychange', onVisible)
  })

  onBeforeUnmount(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', onVisible)
  })

  return { enabled }
}
