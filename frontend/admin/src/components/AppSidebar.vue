<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import type { MenuNode, Session } from '@/types'
import { roleName } from '@/utils/role'

const props = defineProps<{ brand: string; session: Session; menu: MenuNode[] }>()
const emit = defineEmits<{ logout: []; navigate: [] }>()

const route = useRoute()

// 已收合的群組
const closed = ref(new Set<string>())

function toggle(code: string) {
  const next = new Set(closed.value)
  if (next.has(code)) {
    next.delete(code)
  } else {
    next.add(code)
  }
  closed.value = next
}

function holdsCurrent(n: MenuNode): boolean {
  return n.path === route.path || n.children.some(holdsCurrent)
}

// 換頁時自動展開所在群組
watch(
  () => route.path,
  () => {
    const next = new Set(closed.value)
    for (const g of props.menu) {
      if (holdsCurrent(g)) {
        next.delete(g.code)
      }
    }
    closed.value = next
  },
)

const initial = computed(() => props.session.username.slice(0, 1).toUpperCase())

// 滑塊位置，單一元素在各列之間滑動
const nav = ref<HTMLElement | null>(null)
const slider = ref<{ top: number; height: number } | null>(null)
const ready = ref(false)
const hovering = ref(false)

function moveTo(el: Element | null) {
  if (el instanceof HTMLElement) {
    slider.value = { top: el.offsetTop, height: el.offsetHeight }
  } else {
    slider.value = null
  }
}

// 回到目前頁面所在的列
function rest() {
  hovering.value = false
  moveTo(nav.value?.querySelector('a.router-link-exact-active') ?? null)
}

function onOver(e: Event) {
  const row = (e.target as HTMLElement).closest('.row')
  if (row) {
    hovering.value = true
    moveTo(row)
  }
}

async function settle() {
  await nextTick()
  rest()
}

watch(() => route.path, settle)
watch(closed, settle)
watch(() => props.menu, settle, { deep: true })
onMounted(async () => {
  await settle()
  // 第一次定位不播動畫
  requestAnimationFrame(() => (ready.value = true))
})
</script>

<template>
  <aside class="side" aria-label="側邊選單">
    <div class="brand">Argo <span>{{ brand }}</span></div>

    <div class="user">
      <span class="avatar" aria-hidden="true">{{ initial }}</span>
      <div>
        <div class="name">{{ session.username }}</div>
        <div class="role">{{ roleName(session.role) }}</div>
      </div>
    </div>

    <nav ref="nav" @pointerover="onOver" @pointerleave="rest" @focusin="onOver" @focusout="rest">
      <span
        v-if="slider"
        class="slider"
        :class="{ ready, hover: hovering }"
        :style="{ transform: `translateY(${slider.top}px)`, height: `${slider.height}px` }"
        aria-hidden="true"
      />
      <ul v-if="menu.length > 0" class="list">
        <li v-for="n in menu" :key="n.code">
          <RouterLink v-if="n.path && n.children.length === 0" :to="n.path" class="row item" @click="emit('navigate')">
            {{ n.title }}
          </RouterLink>
          <template v-else>
            <button
              type="button"
              class="row group"
              :aria-expanded="!closed.has(n.code)"
              @click="toggle(n.code)"
            >
              <span>{{ n.title }}</span>
              <span class="arrow" :class="{ shut: closed.has(n.code) }" aria-hidden="true">▾</span>
            </button>
            <ul v-show="!closed.has(n.code)" class="sub">
              <li v-for="c in n.children.filter((x) => x.path)" :key="c.code">
                <RouterLink :to="c.path!" class="row item" @click="emit('navigate')">
                  {{ c.title }}
                </RouterLink>
              </li>
            </ul>
          </template>
        </li>
      </ul>
      <p v-else class="none">目前尚無可用功能</p>
    </nav>

    <div class="foot">
      <button type="button" class="logout" @click="emit('logout')">登出</button>
    </div>
  </aside>
</template>

<style scoped>
.side {
  display: flex;
  flex-direction: column;
  width: 232px;
  height: 100%;
  background: #1f2430;
  color: #e5e7eb;
}

.brand {
  padding: 18px 20px;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-bottom: 1px solid rgb(255 255 255 / 10%);
}

.brand span {
  display: block;
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid rgb(255 255 255 / 10%);
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-weight: 700;
}

.name {
  font-weight: 600;
  overflow-wrap: anywhere;
}

.role {
  font-size: 12px;
  color: #9ca3af;
}

nav {
  position: relative;
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.list,
.sub {
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 滑塊，停在目前頁面，滑鼠移上去時跟著走 */
.slider {
  position: absolute;
  top: 0;
  right: 8px;
  left: 8px;
  z-index: 0;
  border-radius: 8px;
  background: var(--color-primary);
  box-shadow: 0 2px 8px rgb(0 0 0 / 25%);
  pointer-events: none;
}

.slider.ready {
  transition:
    transform 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    height 0.25s cubic-bezier(0.4, 0, 0.2, 1),
    background-color 0.2s;
}

.slider.hover {
  background: rgb(255 255 255 / 12%);
  box-shadow: none;
}

/* 父層與子層同樣大小，只用縮排區分 */
.row {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 40px;
  margin: 2px 0;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #cbd5e1;
  font-size: 14px;
  line-height: 1;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.2s;
}

.row:hover,
.row:focus-visible {
  color: #fff;
  outline: none;
}

.group {
  font-weight: 600;
}

.sub .row {
  padding-left: 28px;
}

.item.router-link-exact-active {
  color: #fff;
  font-weight: 600;
}

.arrow {
  transition: transform 0.2s;
}

.arrow.shut {
  transform: rotate(-90deg);
}

.none {
  margin: 0;
  padding: 12px 20px;
  font-size: 13px;
  color: #9ca3af;
}

.foot {
  padding: 12px 20px;
  border-top: 1px solid rgb(255 255 255 / 10%);
}

.logout {
  width: 100%;
  background: transparent;
  border-color: rgb(255 255 255 / 25%);
  color: #e5e7eb;
}

.logout:hover {
  background: rgb(255 255 255 / 8%);
}

@media (prefers-reduced-motion: reduce) {
  .slider.ready,
  .row,
  .arrow {
    transition: none;
  }
}
</style>
