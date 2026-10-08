import { createRouter, createWebHistory } from 'vue-router'

// 實際頁面由 PageHost 依選單決定
export function createStaffRouter() {
  return createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [{ path: '/:pathMatch(.*)*', component: { render: () => null } }],
  })
}
