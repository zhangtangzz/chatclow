import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'Chat',
    component: () => import('../views/chat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('../views/Knowledge.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/token-stats',
    name: 'TokenStats',
    component: () => import('../views/TokenStats.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：
// 1. 未登录 → 跳转 /login
// 2. 非管理员访问 /admin → 跳转 /
// 3. 已登录访问 /login → 根据 role 跳转对应首页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = Number(localStorage.getItem('role'))

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresAdmin && role !== 2) {
    // 非管理员禁止访问管理后台
    next('/')
  } else if (to.path === '/login' && token) {
    // 已登录，根据角色跳转
    next(role === 2 ? '/admin' : '/')
  } else {
    next()
  }
})

export default router
