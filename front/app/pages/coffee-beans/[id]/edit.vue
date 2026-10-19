<script setup lang="ts">
const route = useRoute()
</script>
<template><RecordForm kind="beans" :id="String(route.params.id)" /></template>
