<!-- Editorial section label: thin rule lines flanking tracked small-caps text. -->
<template>
  <div class="section-label">
    <span class="rule" />
    <span class="small-caps text"><slot /></span>
    <span class="rule" />
  </div>
</template>

<style scoped>
.section-label {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.rule {
  height: 1px;
  flex: 1;
  background: var(--border);
}
.text {
  color: var(--accent);
  white-space: nowrap;
}
</style>
