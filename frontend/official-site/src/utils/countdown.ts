import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'

// 倒數到指定時間，每秒更新
export function useCountdown(target: Ref<string | undefined>, onZero: () => void) {
  const now = ref(Date.now())
  let timer: ReturnType<typeof setInterval> | undefined
  let fired = false

  const remainingMs = computed(() =>
    target.value ? Math.max(0, new Date(target.value).getTime() - now.value) : 0,
  )

  const label = computed(() => {
    const total = Math.ceil(remainingMs.value / 1000)
    const m = String(Math.floor(total / 60)).padStart(2, '0')
    const s = String(total % 60).padStart(2, '0')
    return `${m}:${s}`
  })

  function stop() {
    clearInterval(timer)
    timer = undefined
  }

  watch(
    target,
    (value) => {
      stop()
      fired = false
      if (!value) return
      now.value = Date.now()
      timer = setInterval(() => {
        now.value = Date.now()
        if (remainingMs.value <= 0 && !fired) {
          fired = true
          stop()
          onZero()
        }
      }, 1000)
    },
    { immediate: true },
  )

  onBeforeUnmount(stop)
  return { remainingMs, label }
}
