<script setup>
defineProps({
  label: String,
  id: { type: String, required: true },
  type: { type: String, default: 'text' },
  modelValue: { type: String, default: '' },
  placeholder: String,
  autocomplete: String,
  inputmode: String,
  maxlength: [String, Number],
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="field">
    <label v-if="label" :for="id" class="small-caps label">{{ label }}</label>
    <input
      :id="id"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :inputmode="inputmode"
      :maxlength="maxlength"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  </div>
</template>

<style scoped>
.field {
  display: block;
}
.label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--muted-foreground);
}
input {
  width: 100%;
  height: 48px;
  padding: 0 0.875rem;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--foreground);
  transition: all 150ms ease-out;
}
input::placeholder {
  color: var(--muted-foreground);
  opacity: 0.6;
}
input:hover {
  border-color: var(--border-hover);
}
input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--background), 0 0 0 4px var(--ring);
}
</style>
