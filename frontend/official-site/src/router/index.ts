import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
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

export default router
