import { VueConstructor } from 'vue';
import ApiService from '@/services/ApiService';

interface ApiHandlerPlugin {
  install(Vue: VueConstructor): void;
}

const ApiHandlerPlugin: ApiHandlerPlugin = {
  install(Vue: VueConstructor) {
    Vue.prototype.$api = ApiService;
    (Vue as any).$api = ApiService;
    
    // Add convenient methods
    Vue.prototype.$sendMessage = async function(message: string, sessionId: string) {
      try {
        return await ApiService.sendMessage(message, sessionId);
      } catch (error: any) {
        if (this.$bvToast) {
          this.$bvToast.toast(error.message, {
            title: 'API Error',
            variant: 'danger',
            solid: true,
          });
        }
        throw error;
      }
    };
    
    Vue.prototype.$checkHealth = async function() {
      try {
        return await ApiService.healthCheck();
      } catch (error: any) {
        if (this.$bvToast) {
          this.$bvToast.toast('Server connection failed', {
            title: 'Connection Error',
            variant: 'warning',
            solid: true,
          });
        }
        throw error;
      }
    };
  },
};

export default ApiHandlerPlugin;