import Vue from 'vue';
import Router, { RouteConfig } from 'vue-router';
import ChatRoom from '@/views/ChatRoom.vue';
import ChatRoomList from '@/views/ChatRoomList.vue';

Vue.use(Router);

const routes: RouteConfig[] = [
  {
    path: '/',
    name: 'Home',
    component: ChatRoomList,
  },
  {
    path: '/chatrooms',
    name: 'ChatRooms',
    component: ChatRoomList,
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
  base: '/',
  routes,
});

export default router;