import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue') },
    {
      path: '/account',
      name: 'account',
      meta: { requiresAuth: true },
      component: () => import('@/views/AccountView.vue'),
    },
    { path: '/cart', name: 'cart', component: () => import('@/views/CartView.vue') },
    { path: '/checkout', name: 'checkout', component: () => import('@/views/CheckoutView.vue') },
    { path: '/orders', name: 'orders', component: () => import('@/views/OrderLookupView.vue') },
    {
      path: '/orders/:orderNo',
      name: 'orderDetail',
      component: () => import('@/views/OrderDetailView.vue'),
    },
    {
      path: '/cards',
      name: 'cards',
      component: () => import('@/views/CardListView.vue'),
    },
    {
      path: '/cards/:id(\\d+)',
      name: 'cardDetail',
      component: () => import('@/views/CardDetailView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

// 需要登入的頁面導向登入頁，已登入就不用再看登入與註冊頁
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'login' || to.name === 'register') && auth.isLoggedIn) {
    return { name: 'account' }
  }
})

export default router
