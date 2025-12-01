import Vue from 'vue';
import axios from 'axios';
import VueCookies from 'vue-cookies';
import socketIOClient from 'socket.io-client';
import BootstrapVue from 'bootstrap-vue';
import store from './store';
import router from './router/index';
import App from './App.vue';
// @ts-ignore
import VueSweetalert2 from 'vue-sweetalert2';
// @ts-ignore
import Toasted from 'vue-toasted';
// @ts-ignore
import VueHead from 'vue-head';

import i18n from './plugins/i18n';
// @ts-ignore
import Notifications from 'vue-notification';
import Loader from './components/Loader.vue';
import './plugins/interceptor';
import ApiHandler from './plugins/ApiHandler';

import './assets/overrides.scss';
import './assets/main.scss';
import 'vue-material-design-icons/styles.css';
import 'sweetalert2/dist/sweetalert2.min.css';

Vue.config.productionTip = false;

// Type augmentation for Vue prototype
declare module 'vue/types/vue' {
  interface Vue {
    $http: typeof axios;
    $socketIOClient: typeof socketIOClient;
  }
}

Vue.prototype.$http = axios;
Vue.prototype.$socketIOClient = socketIOClient;

Vue.use(BootstrapVue);
Vue.use(VueCookies as any);
Vue.use(Notifications);
Vue.use(VueSweetalert2);
Vue.use(VueHead);
Vue.use(Toasted, {
  duration: 5000,
  action: {
    text: 'Close',
    onClick: (e: any, toastObject: any) => {
      toastObject.goAway(0);
    },
  },
});
Vue.use(ApiHandler);
Vue.component('loader', Loader);

new Vue({
  router,
  store,
  i18n,
  render: (h) => h(App),
}).$mount('#app');