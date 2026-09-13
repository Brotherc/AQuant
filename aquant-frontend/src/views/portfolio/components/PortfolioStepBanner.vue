<template>
  <div class="step-guide-banner">
    <div class="steps-wrapper">
      <template v-for="(step, index) in steps" :key="step.number">
        <div
          class="step-item"
          :class="{
            'is-completed': step.status === 'completed',
            'is-current': step.status === 'current',
            'is-pending': step.status === 'pending'
          }"
          @click="$emit('stepClick', step.number)"
        >
          <div class="step-icon-wrap">
            <div v-if="step.status === 'completed'" class="step-icon step-icon-completed">
              <svg viewBox="0 0 1024 1024" width="14" height="14">
                <path
                  d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.8c4.1-5.1.4-12.9-6.3-12.9z"
                  fill="#ffffff"
                />
              </svg>
            </div>
            <div v-else-if="step.status === 'current'" class="step-icon step-icon-current">
              {{ step.number }}
            </div>
            <div v-else class="step-icon step-icon-pending">
              {{ step.number }}
            </div>
          </div>
          <div class="step-info">
            <div class="step-title">{{ step.number }}. {{ step.title }}</div>
            <div class="step-desc">{{ step.desc }}</div>
          </div>
        </div>

        <!-- 步骤之间的水平连接线 -->
        <div
          v-if="index < steps.length - 1"
          class="step-connector"
          :class="{ 'is-active': step.status === 'completed' }"
        ></div>
      </template>
    </div>

    <div class="banner-action-side">
      <button class="banner-cta-btn" @click="$emit('ctaClick')">
        <span>{{ ctaText }}</span>
        <svg viewBox="0 0 1024 1024" width="14" height="14" class="btn-arrow">
          <path
            d="M869 487.8L491.2 159.9c-9.9-8.6-25-1.6-25 11.6v88.2c0 7 3.2 13.6 8.7 17.9L709.7 480H164c-13.3 0-24 10.7-24 24v48c0 13.3 10.7 24 24 24h545.7L474.9 746.4c-5.5 4.3-8.7 10.9-8.7 17.9v88.2c0 13.1 15.1 20.2 25 11.6l377.8-327.9c10.4-8.9 10.4-25.5 0-34.4z"
            fill="currentColor"
          />
        </svg>
      </button>
      <div class="banner-sub-text">{{ ctaSubText }}</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    hasPortfolio?: boolean;
    hasAccount?: boolean;
    hasTrade?: boolean;
    ctaText?: string;
    ctaSubText?: string;
  }>(),
  {
    hasPortfolio: true,
    hasAccount: true,
    hasTrade: true,
    ctaText: '查看持仓',
    ctaSubText: '已完成全部设置，开始管理您的持仓'
  }
);

defineEmits<{
  (e: 'stepClick', stepNumber: number): void;
  (e: 'ctaClick'): void;
}>();

const steps = computed(() => [
  {
    number: 1,
    title: '创建组合',
    desc: '创建您的投资组合',
    status: props.hasPortfolio ? 'completed' : 'current'
  },
  {
    number: 2,
    title: '添加券商账户',
    desc: '关联券商资金账户',
    status: props.hasPortfolio ? (props.hasAccount ? 'completed' : 'current') : 'pending'
  },
  {
    number: 3,
    title: '导入交易流水',
    desc: '导入历史交易数据',
    status: props.hasAccount ? (props.hasTrade ? 'completed' : 'current') : 'pending'
  },
  {
    number: 4,
    title: '查看持仓',
    desc: '查看持仓与收益情况',
    status: props.hasTrade ? 'current' : 'pending'
  }
]);
</script>

<style scoped>
.step-guide-banner {
  background: #ffffff;
  border-radius: 12px;
  padding: 18px 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 20px;
}

.steps-wrapper {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: space-between;
  gap: 16px;
  max-width: 820px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
}

.step-item:hover {
  opacity: 0.85;
}

.step-connector {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
  min-width: 16px;
  max-width: 48px;
  margin: 0 4px;
}

.step-icon-wrap {
  flex-shrink: 0;
}

.step-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.step-icon-completed {
  background: #0f172a;
  color: #ffffff;
}

.step-icon-current {
  background: #0f172a;
  color: #ffffff;
}

.step-icon-pending {
  background: #f1f5f9;
  color: #94a3b8;
  border: 1px solid #e2e8f0;
}

.step-info {
  display: flex;
  flex-direction: column;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.3;
}

.is-pending .step-title {
  color: #64748b;
  font-weight: 500;
}

.step-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.banner-action-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  min-width: 170px;
}

.banner-cta-btn {
  background: #0f172a;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 8px 24px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
  width: 100%;
  justify-content: center;
}

.banner-cta-btn:hover {
  background: #1e293b;
  transform: translateY(-1px);
}

.btn-arrow {
  margin-left: 2px;
}

.banner-sub-text {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 6px;
  text-align: center;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .step-guide-banner {
    flex-direction: column;
    align-items: stretch;
  }
  .steps-wrapper {
    flex-wrap: wrap;
    gap: 16px;
  }
  .banner-action-side {
    align-items: flex-start;
    width: 100%;
  }
}
</style>
