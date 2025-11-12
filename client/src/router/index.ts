import Vue from 'vue';
import Router, { RouteConfig } from 'vue-router';
import ChatRoom from '@/views/ChatRoom.vue';

Vue.use(Router);

const routes: RouteConfig[] = [
  {
    path: '/',
    name: 'Home',
    component: ChatRoom,
  },
  {
    path: '/chat',
    name: 'Chat',
    component: ChatRoom,
  },
  {
    path: '/chat/:sessionId',
    name: 'ChatSession',
    component: ChatRoom,
    props: true,
  },
];

const router = new Router({
  mode: 'history',
  base: import.meta.env.BASE_URL || '/',
  routes,
});

export default router;