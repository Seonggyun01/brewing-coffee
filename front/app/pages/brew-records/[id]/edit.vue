<script setup lang="ts">
const route = useRoute()
</script>
<template><RecordForm kind="brews" :id="String(route.params.id)" /></template>
