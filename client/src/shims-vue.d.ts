declare module '*.vue' {
  import Vue from 'vue';
  export default Vue;
}

declare module 'vue-cookies' {
  interface VueCookies {
    get(key: string): any;
    set(key: string, value: any, expiry?: any): void;
    remove(key: string): void;
    isKey(key: string): boolean;
    keys(): string[];
  }
  const VueCookies: VueCookies;
  export = VueCookies;
}

declare module 'vue-material-design-icons/*' {
  import Vue from 'vue';
  export default Vue;
}

declare module 'vue/types/vue' {
  interface Vue {
    $api: any;
    $sendMessage: (message: string, sessionId: string) => Promise<any>;
    $checkHealth: () => Promise<any>;
    $bvToast: any;
  }
}