<script setup lang="ts">
const route = useRoute()
</script>
<template><RecordDetail kind="beans" :id="String(route.params.id)" /></template>
