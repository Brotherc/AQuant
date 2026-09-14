<template>
  <div class="account-fund-banner">
    <div class="banner-left-content">
      <div class="wallet-icon-box">
        <svg viewBox="0 0 1024 1024" width="48" height="48" fill="#94a3b8">
          <path d="M880 208H144c-17.7 0-32 14.3-32 32v544c0 17.7 14.3 32 32 32h736c17.7 0 32-14.3 32-32V240c0-17.7-14.3-32-32-32zm-40 536H184V280h656v464z" />
          <path d="M680 448h120v128H680z" fill="#0f172a" />
          <path d="M256 368h200v48H256zm0 112h320v48H256z" fill="#cbd5e1" />
        </svg>
      </div>
      <div class="banner-text-wrap">
        <div class="banner-main-title">先创建组合，再绑定券商资金账户</div>
        <div class="banner-main-desc">
          创建投资组合用于管理您的持仓资产，然后添加券商资金账户，关联您的交易数据。完成前两步后，即可导入交易流水，开始查看持仓分析与收益情况。
        </div>
      </div>
    </div>

    <div class="banner-right-steps">
      <div class="step-check-row">
        <div class="check-icon-circle">
          <svg viewBox="0 0 1024 1024" width="12" height="12">
            <path
              d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.8c4.1-5.1.4-12.9-6.3-12.9z"
              fill="#ffffff"
            />
          </svg>
        </div>
        <div class="step-label-wrap">
          <div class="step-name">创建投资组合</div>
          <div class="step-hint">设置组合名称、基准指数等</div>
        </div>
        <div class="step-status-tag">已完成</div>
      </div>

      <div class="step-check-row">
        <div class="check-icon-circle">
          <svg viewBox="0 0 1024 1024" width="12" height="12">
            <path
              d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474a32 32 0 0 0-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9l273.9 347c12.8 16.2 37.4 16.2 50.3 0l488.4-618.8c4.1-5.1.4-12.9-6.3-12.9z"
              fill="#ffffff"
            />
          </svg>
        </div>
        <div class="step-label-wrap">
          <div class="step-name">添加券商账户</div>
          <div class="step-hint">关联您的券商资金账户</div>
        </div>
        <div class="step-status-tag">已完成</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  hasPortfolio?: boolean;
  hasAccount?: boolean;
}>();
</script>

<style scoped>
.account-fund-banner {
  background: #ffffff;
  border-radius: 10px;
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 24px;
}

.banner-left-content {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.wallet-icon-box {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid #f1f5f9;
}

.banner-text-wrap {
  display: flex;
  flex-direction: column;
}

.banner-main-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.banner-main-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  margin-top: 6px;
  max-width: 580px;
}

.banner-right-steps {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 280px;
  padding-left: 24px;
  border-left: 1px solid #f1f5f9;
}

.step-check-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.check-icon-circle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #0f172a;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-label-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.step-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.step-hint {
  font-size: 11px;
  color: #94a3b8;
}

.step-status-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f0fdf4;
  color: #16a34a;
  font-weight: 500;
}

@media (max-width: 992px) {
  .account-fund-banner {
    flex-direction: column;
    align-items: stretch;
  }
  .banner-right-steps {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid #f1f5f9;
    padding-top: 16px;
  }
}
</style>
