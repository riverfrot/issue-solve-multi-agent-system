<template>
  <div class="loader-wrapper" :class="{ 'loader-fullscreen': fullscreen }">
    <div class="loader-content">
      <b-spinner 
        :variant="variant" 
        :small="small"
        :label="label || 'Loading...'"
      />
      <div v-if="text" class="loader-text mt-2">
        {{ text }}
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop } from 'vue-property-decorator';

@Component
export default class Loader extends Vue {
  @Prop({ type: String, default: 'primary' })
  variant!: string;

  @Prop({ type: Boolean, default: false })
  small!: boolean;

  @Prop({ type: Boolean, default: false })
  fullscreen!: boolean;

  @Prop({ type: String, default: null })
  text!: string | null;

  @Prop({ type: String, default: 'Loading...' })
  label!: string;
}
</script>

<style lang="scss" scoped>
.loader-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  
  &.loader-fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(255, 255, 255, 0.8);
    z-index: 9999;
  }
}

.loader-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.loader-text {
  font-size: 0.9rem;
  color: #6c757d;
  text-align: center;
}
</style>