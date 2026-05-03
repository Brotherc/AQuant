<template>
  <div class="finance-sites-page">
    <a-card :bordered="false">
      <template #title>常用网站</template>
      <a-row :gutter="[16, 16]">
        <a-col v-for="site in financeSites" :key="site.url" :xs="24" :sm="12" :xl="8">
          <a
            class="finance-site-card"
            :href="site.url"
            target="_blank"
            rel="noopener noreferrer"
          >
            <div class="finance-site-card__icon">
              <img
                v-if="!brokenSiteIcons[site.url]"
                :src="site.iconUrl"
                :alt="`${site.name} logo`"
                class="finance-site-card__icon-image"
                @error="handleIconError(site.url)"
              />
              <GlobalOutlined v-else />
            </div>
            <div class="finance-site-card__content">
              <div class="finance-site-card__title">
                {{ site.name }}
                <ExportOutlined class="finance-site-card__jump" />
              </div>
              <div class="finance-site-card__desc">{{ site.description }}</div>
              <div class="finance-site-card__url">{{ site.url }}</div>
            </div>
          </a>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ExportOutlined, GlobalOutlined } from '@ant-design/icons-vue';

type FinanceSite = {
  name: string;
  url: string;
  description: string;
  iconUrl: string;
};

const createSite = (name: string, url: string, description: string, iconUrl?: string): FinanceSite => ({
  name,
  url,
  description,
  iconUrl: iconUrl ?? `${new URL(url).origin}/favicon.ico`
});

const financeSites = [
  createSite('虎嗅', 'https://www.huxiu.com', '商业、科技与财经资讯入口'),
  createSite('英为财情', 'https://cn.investing.com', '全球市场行情、宏观数据与投资工具'),
  createSite('ValueCell', 'https://github.com/ValueCell-ai/valuecell', '面向金融应用的社区驱动型多智能体平台', 'https://github.com/fluidicon.png'),
  createSite('Kronos', 'https://github.com/shiyu-coder/Kronos', '面向金融市场语言的基础模型', 'https://github.com/fluidicon.png'),
  createSite('TradingAgents', 'https://github.com/TauricResearch/TradingAgents', '多智能体大语言模型金融交易框架', 'https://github.com/fluidicon.png'),
  createSite('AI Hedge Fund', 'https://github.com/virattt/ai-hedge-fund', '一个 AI 对冲基金团队', 'https://github.com/fluidicon.png'),
  createSite('大盘云图', 'https://52etf.site', '大盘云图 - A股热力图')
];

const brokenSiteIcons = ref<Record<string, boolean>>({});

const handleIconError = (siteUrl: string) => {
  brokenSiteIcons.value[siteUrl] = true;
};
</script>

<style scoped>
.finance-sites-page {
  display: flex;
  flex-direction: column;
}

.finance-site-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  height: 100%;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #ffffff, #fafcff);
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.finance-site-card:hover {
  transform: translateY(-2px);
  border-color: rgba(24, 144, 255, 0.28);
  box-shadow: 0 10px 24px rgba(15, 41, 77, 0.08);
}

.finance-site-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(24, 144, 255, 0.1);
  color: #1890ff;
  font-size: 20px;
  flex-shrink: 0;
  overflow: hidden;
}

.finance-site-card__icon-image {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.finance-site-card__content {
  min-width: 0;
  flex: 1;
}

.finance-site-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #1f1f1f;
  font-size: 17px;
  font-weight: 700;
}

.finance-site-card__jump {
  color: #8c8c8c;
  font-size: 13px;
}

.finance-site-card__desc {
  margin-bottom: 8px;
  color: #595959;
  font-size: 13px;
  line-height: 1.7;
}

.finance-site-card__url {
  color: #8c8c8c;
  font-size: 12px;
  word-break: break-all;
}

</style>
