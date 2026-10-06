<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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

    <nav>
      <ul v-if="menu.length > 0" class="list">
        <li v-for="n in menu" :key="n.code">
          <RouterLink v-if="n.path && n.children.length === 0" :to="n.path" class="item" @click="emit('navigate')">
            {{ n.title }}
          </RouterLink>
          <template v-else>
            <button
              type="button"
              class="group"
              :aria-expanded="!closed.has(n.code)"
              @click="toggle(n.code)"
            >
              <span>{{ n.title }}</span>
              <span class="arrow" :class="{ shut: closed.has(n.code) }" aria-hidden="true">▾</span>
            </button>
            <ul v-show="!closed.has(n.code)" class="sub">
              <li v-for="c in n.children.filter((x) => x.path)" :key="c.code">
                <RouterLink :to="c.path!" class="item" @click="emit('navigate')">
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
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.list,
.sub {
  margin: 0;
  padding: 0;
  list-style: none;
}

.group {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 10px 20px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: #9ca3af;
  font-size: 13px;
  text-align: left;
}

.group:hover {
  color: #fff;
}

.arrow {
  transition: transform 0.15s;
}

.arrow.shut {
  transform: rotate(-90deg);
}

.item {
  display: block;
  padding: 9px 20px 9px 32px;
  border-left: 3px solid transparent;
  color: #e5e7eb;
}

.list > li > .item {
  padding-left: 20px;
}

.item:hover {
  background: rgb(255 255 255 / 6%);
}

.item.router-link-exact-active {
  border-left-color: var(--color-primary);
  background: rgb(255 255 255 / 10%);
  color: #fff;
  font-weight: 600;
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
</style>
