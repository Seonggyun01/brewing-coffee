<script setup lang="ts">
const props = defineProps<{
  label: string
  name: string
  type?: string
  required?: boolean
  options?: { value: string; label: string }[]
  min?: number
  max?: number
  step?: string
  placeholder?: string
  error?: string
}>()
const model = defineModel<any>()
</script>
<template>
  <div class="field">
    <label :for="name"
      >{{ label }}
      <span :class="required ? 'required' : 'optional'">{{ required ? '*' : '선택' }}</span></label
    ><select v-if="options" :id="name" v-model="model" :required="required" :aria-invalid="!!error">
      <option value="">선택해주세요</option>
      <option v-for="o in options" :key="o.value" :value="o.value">{{ o.label }}</option></select
    ><textarea
      v-else-if="type === 'textarea'"
      :id="name"
      v-model="model"
      :placeholder="placeholder"
      :maxlength="max"
      :aria-invalid="!!error"
    /><input
      v-else
      :id="name"
      v-model="model"
      :type="type || 'text'"
      :required="required"
      :min="min"
      :max="type === 'number' ? max : undefined"
      :maxlength="type !== 'number' ? max : undefined"
      :step="step"
      :placeholder="placeholder"
      :aria-invalid="!!error"
    /><small v-if="error" class="error-text">{{ error }}</small>
  </div>
</template>
