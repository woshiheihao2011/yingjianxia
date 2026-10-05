<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppNav from '@/components/AppNav.vue'
import AppFooter from '@/components/AppFooter.vue'

const route = useRoute()

const isFullPage = computed(() => ['Auth', 'NotFound'].includes(route.name))
</script>

<template>
  <div class="min-h-screen flex flex-col bg-brand-bg text-brand-ink">
    <AppNav v-if="!isFullPage" />
    <main :class="isFullPage ? 'flex-1' : 'flex-1 pt-4 pb-8'" class="w-full">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <AppFooter v-if="!isFullPage" />

    <!-- 全局 Toast 占位（后续集成 Toast Store） -->
    <div id="toast-root" class="fixed bottom-6 right-6 z-[100] space-y-2"></div>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .18s ease, transform .18s ease; }
.fade-enter-from { opacity: 0; transform: translateY(6px); }
.fade-leave-to   { opacity: 0; transform: translateY(-6px); }
</style>
