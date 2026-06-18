<script setup>
defineProps({
  accentTop: { type: Boolean, default: false },
})
</script>

<template>
  <div :class="['card', { 'accent-top': accentTop }]">
    <slot />
  </div>
</template>

<style scoped>
.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow: var(--shadow-md);
}
.accent-top {
  border-top: 2px solid var(--accent);
}
</style>
