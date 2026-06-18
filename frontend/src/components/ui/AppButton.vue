<script setup>
defineProps({
  variant: { type: String, default: 'primary' }, // primary | secondary | ghost
  type: { type: String, default: 'button' },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
})
</script>

<template>
  <button :type="type" :disabled="disabled" :class="['btn', variant, { block }]">
    <slot />
  </button>
</template>

<style scoped>
.btn {
  font-family: var(--font-body);
  font-size: 0.95rem;
  font-weight: 500;
  letter-spacing: 0.05em;
  min-height: 44px;
  padding: 0 1.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 200ms ease-out;
  touch-action: manipulation;
}
.btn.block {
  width: 100%;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.primary {
  background: var(--accent);
  color: var(--accent-foreground);
  border: 1px solid var(--accent);
  box-shadow: var(--shadow-sm);
}
.primary:not(:disabled):hover {
  background: var(--accent-secondary);
  border-color: var(--accent-secondary);
  box-shadow: var(--shadow-md);
}

.secondary {
  background: transparent;
  color: var(--foreground);
  border: 1px solid var(--foreground);
}
.secondary:not(:disabled):hover {
  background: var(--muted);
  color: var(--accent);
  border-color: var(--accent);
}

.ghost {
  background: none;
  border: none;
  color: var(--muted-foreground);
  min-height: auto;
  padding: 0.25rem 0;
  letter-spacing: 0.02em;
}
.ghost:not(:disabled):hover {
  color: var(--foreground);
  text-decoration: underline;
  text-decoration-color: var(--accent);
  text-underline-offset: 4px;
}

.btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--background), 0 0 0 4px var(--ring);
}
</style>
