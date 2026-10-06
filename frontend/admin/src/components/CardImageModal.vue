<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

defineProps<{ src: string | null; name: string }>()
const emit = defineEmits<{ close: [] }>()

const box = ref<HTMLElement | null>(null)

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    emit('close')
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
  box.value?.focus()
})
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div class="backdrop" @click.self="emit('close')">
      <div ref="box" class="box" role="dialog" aria-modal="true" :aria-label="name" tabindex="-1">
        <img v-if="src" :src="src" :alt="name" referrerpolicy="no-referrer" />
        <p v-else class="none">這張卡沒有圖片</p>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(0 0 0 / 55%);
}

.box {
  outline: none;
}

/* 卡圖約占螢幕面積六分之一，窄螢幕時以寬度為限 */
img {
  display: block;
  height: min(64vh, calc(90vw * 88 / 63));
  aspect-ratio: 63 / 88;
  border-radius: 12px;
  background: #f1f1ee;
  box-shadow: 0 10px 40px rgb(0 0 0 / 45%);
}

.none {
  padding: 24px 32px;
  border-radius: 8px;
  background: #fff;
  color: var(--color-muted);
}
</style>
