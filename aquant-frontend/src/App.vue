<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="{ token: { colorPrimary: '#000000', colorInfo: '#000000' } }">
    <router-view />
  </a-config-provider>
</template>

<style scoped>
</style>
