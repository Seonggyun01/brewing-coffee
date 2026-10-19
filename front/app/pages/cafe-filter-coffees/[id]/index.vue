<script setup lang="ts">
const route = useRoute()
</script>
<template><RecordDetail kind="cafes" :id="String(route.params.id)" /></template>
