<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ApiError, getMenu } from '@/api/ops'
import type { MenuNode, Session } from '@/types'
import { errorText } from '@/utils/error'
import { trailOf } from '@/utils/menu'
import AppSidebar from './AppSidebar.vue'
import PageHost from './PageHost.vue'

const props = defineProps<{ brand: string; session: Session }>()
const emit = defineEmits<{ logout: []; expired: [] }>()

const route = useRoute()
const menu = ref<MenuNode[]>([])
const loaded = ref(false)
const error = ref('')
const drawer = ref(false)

const trail = computed(() => trailOf(menu.value, route.path))

async function load() {
  error.value = ''
  try {
    menu.value = await getMenu(props.session.token)
    loaded.value = true
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('expired')
      return
    }
    error.value = errorText(e)
  }
}

onMounted(load)
</script>

<template>
  <div class="shell" :class="{ open: drawer }">
    <AppSidebar
      class="side"
      :brand="brand"
      :session="session"
      :menu="menu"
      @logout="emit('logout')"
      @navigate="drawer = false"
    />
    <div class="scrim" @click="drawer = false"></div>

    <div class="main">
      <header class="top">
        <button type="button" class="burger" aria-label="開關選單" :aria-expanded="drawer" @click="drawer = !drawer">
          ☰
        </button>
        <nav class="crumbs" aria-label="目前位置">
          <template v-for="(n, i) in trail" :key="n.code">
            <span v-if="i > 0" aria-hidden="true"> / </span>
            <span :aria-current="i === trail.length - 1 ? 'page' : undefined" :class="{ now: i === trail.length - 1 }">
              {{ n.title }}
            </span>
          </template>
        </nav>
      </header>

      <main class="content">
        <div v-if="error" class="error" role="alert">
          {{ error }}
          <button type="button" @click="load">重試</button>
        </div>
        <p v-else-if="!loaded" class="hint">載入中</p>
        <PageHost v-else :menu="menu" :token="session.token" @unauthorized="emit('expired')" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
}

.side {
  position: sticky;
  top: 0;
  flex: none;
  height: 100vh;
}

.scrim {
  display: none;
}

.main {
  flex: 1;
  min-width: 0;
}

.top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  border-bottom: 1px solid var(--color-border);
  background: #fff;
}

.burger {
  display: none;
  padding: 4px 10px;
}

.crumbs {
  color: var(--color-muted);
}

.now {
  color: var(--color-text);
  font-weight: 600;
}

.content {
  padding: 24px;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: #d92d20;
}

@media (max-width: 768px) {
  .side {
    position: fixed;
    z-index: 20;
    left: 0;
    transform: translateX(-100%);
    transition: transform 0.2s;
  }

  .open .side {
    transform: translateX(0);
  }

  .open .scrim {
    display: block;
    position: fixed;
    z-index: 10;
    inset: 0;
    background: rgb(0 0 0 / 40%);
  }

  .burger {
    display: inline-block;
  }

  .top,
  .content {
    padding-inline: 16px;
  }
}
</style>
