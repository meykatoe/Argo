<script setup lang="ts">
import { computed, ref } from 'vue'
import { clockTime, nearestIndex, toDots } from '@/utils/monitor'

const props = defineProps<{
  label: string
  values: number[]
  times: number[]
  unit: string
  max?: number
}>()

const W = 240
const H = 56
const PAD = 6

const dots = computed(() => toDots(props.values, W, H, PAD, props.max))
const line = computed(() => dots.value.map((d) => `${d.x.toFixed(1)},${d.y.toFixed(1)}`).join(' '))
const area = computed(() => {
  const d = dots.value
  const first = d[0]
  const last = d[d.length - 1]
  if (!first || !last || d.length < 2) return ''
  return `${first.x},${H - PAD} ${line.value} ${last.x},${H - PAD}`
})

// 預設最新一筆
const hover = ref<number | null>(null)
const shown = computed(() => hover.value ?? props.values.length - 1)
const dot = computed(() => dots.value[shown.value])

const readout = computed(() => {
  const v = props.values[shown.value]
  const t = props.times[shown.value]
  return v === undefined || t === undefined ? '' : `${v}${props.unit}`
})
const readTime = computed(() => {
  const t = props.times[shown.value]
  return t === undefined ? '' : clockTime(t)
})

// 提示框不超出兩側
const tipLeft = computed(() => Math.min(Math.max(((dot.value?.x ?? 0) / W) * 100, 18), 82))

const summary = computed(() => {
  const v = props.values
  if (v.length === 0) return `${props.label}：尚無資料`
  return `${props.label} 趨勢，最近 ${v.length} 筆，最低 ${Math.min(...v)}${props.unit}，最高 ${Math.max(...v)}${props.unit}`
})

function onMove(e: PointerEvent) {
  const el = e.currentTarget as SVGSVGElement
  const rect = el.getBoundingClientRect()
  if (rect.width === 0) return
  hover.value = nearestIndex(((e.clientX - rect.left) / rect.width) * W, props.values.length, W, PAD)
}

function onKey(e: KeyboardEvent) {
  const n = props.values.length
  if (n === 0) return
  if (e.key === 'ArrowLeft') hover.value = Math.max(0, shown.value - 1)
  else if (e.key === 'ArrowRight') hover.value = Math.min(n - 1, shown.value + 1)
  else if (e.key === 'Escape') hover.value = null
  else return
  e.preventDefault()
}
</script>

<template>
  <div class="spark">
    <svg
      :viewBox="`0 0 ${W} ${H}`"
      role="img"
      tabindex="0"
      :aria-label="summary"
      @pointermove="onMove"
      @pointerleave="hover = null"
      @blur="hover = null"
      @keydown="onKey"
    >
      <line :x1="PAD" :x2="W - PAD" :y1="H - PAD" :y2="H - PAD" class="axis" />
      <template v-if="values.length >= 2">
        <polygon :points="area" class="area" />
        <polyline :points="line" class="line" />
      </template>
      <template v-if="dot">
        <line v-if="hover !== null" :x1="dot.x" :x2="dot.x" :y1="PAD" :y2="H - PAD" class="cross" />
        <circle :cx="dot.x" :cy="dot.y" r="6" class="ring" />
        <circle :cx="dot.x" :cy="dot.y" r="4" class="mark" />
      </template>
    </svg>
    <p v-if="values.length === 0" class="empty">資料收集中</p>
    <div v-else-if="hover !== null" class="tip" :style="{ left: `${tipLeft}%` }">
      <strong>{{ readout }}</strong>
      <span>{{ readTime }}</span>
    </div>
  </div>
</template>

<style scoped>
.spark {
  position: relative;
  --series-1: #2a78d6;
  --surface: #fff;
}

svg {
  display: block;
  width: 100%;
  height: auto;
  overflow: visible;
  outline-offset: 2px;
}

.axis {
  stroke: #e1e0d9;
  stroke-width: 1;
}

.line {
  fill: none;
  stroke: var(--series-1);
  stroke-width: 2;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.area {
  fill: var(--series-1);
  opacity: 0.1;
}

.cross {
  stroke: #c3c2b7;
  stroke-width: 1;
}

.ring {
  fill: var(--surface);
}

.mark {
  fill: var(--series-1);
}

.empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  font-size: 13px;
  color: var(--color-muted);
}

.tip {
  position: absolute;
  top: -34px;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 3px 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  line-height: 1.3;
  white-space: nowrap;
  pointer-events: none;
  box-shadow: 0 2px 6px rgba(11, 11, 11, 0.08);
}

.tip strong {
  color: var(--color-text);
  font-size: 13px;
}

.tip span {
  color: var(--color-muted);
}
</style>
