<script setup lang="ts">
import StaffApp from '@shared/components/StaffApp.vue'
import type { StaffConfig } from '@shared/config'
import { getMenu, login, logout } from './api/admin'
import { pages } from './pages'
import { errorText, lockedMessage } from './utils/error'

const config: StaffConfig = {
  storageKey: 'argo-admin-session',
  brand: '業務後台',
  title: 'Argo 後台',
  pages,
  errors: { errorText, lockedMessage },
  api: { login, logout, getMenu },
}
</script>

<template>
  <StaffApp :config="config" />
</template>
