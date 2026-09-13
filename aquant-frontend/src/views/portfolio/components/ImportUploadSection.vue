<template>
  <div class="import-upload-card">
    <div class="card-title">导入交易数据</div>

    <!-- 3步流程步骤 (垂直排列) -->
    <div class="import-steps-vertical">
      <div class="v-step-item">
        <div class="v-step-left">
          <span class="v-step-num is-active">1</span>
          <span class="v-step-line"></span>
        </div>
        <div class="v-step-content">
          <div class="v-step-title">选择账户与券商</div>
          <div class="v-step-desc">确认要导入数据的券商与目标账户</div>
        </div>
      </div>

      <div class="v-step-item">
        <div class="v-step-left">
          <span class="v-step-num">2</span>
          <span class="v-step-line"></span>
        </div>
        <div class="v-step-content">
          <div class="v-step-title">上传文件</div>
          <div class="v-step-desc">拖拽或点击上传流水文件 (CSV/XLS/XLSX)</div>
        </div>
      </div>

      <div class="v-step-item">
        <div class="v-step-left">
          <span class="v-step-num">3</span>
        </div>
        <div class="v-step-content">
          <div class="v-step-title">完成导入</div>
          <div class="v-step-desc">系统自动解析校验并同步更新持仓与现金</div>
        </div>
      </div>
    </div>

    <!-- 券商类型与目标账户配置栏 -->
    <div class="import-config-row">
      <div class="config-item">
        <span class="config-label">券商类型</span>
        <a-select
          v-model:value="selectedBroker"
          class="config-select broker-select"
          placeholder="请选择券商类型"
          style="width: 100%;"
        >
          <a-select-option value="CMS">招商证券 (支持原始流水)</a-select-option>
          <a-select-option value="STANDARD">标准模板 (CSV / Excel)</a-select-option>
          <a-select-option value="HTSC">华泰证券 (涨乐财富通)</a-select-option>
          <a-select-option value="CITIC">中信证券</a-select-option>
          <a-select-option value="GTJA">国泰君安</a-select-option>
          <a-select-option value="EASTMONEY">东方财富</a-select-option>
          <a-select-option value="THS">同花顺导出流水</a-select-option>
          <a-select-option value="OTHER">其他券商 (标准模板)</a-select-option>
        </a-select>
      </div>

      <div class="config-item" v-if="accounts && accounts.length > 0">
        <span class="config-label">目标账户</span>
        <a-select
          v-model:value="targetAccountId"
          class="config-select account-select"
          placeholder="请选择目标账户"
          style="width: 100%;"
        >
          <a-select-option
            v-for="acc in accounts"
            :key="acc.id"
            :value="acc.id"
          >
            {{ acc.accountName }}
          </a-select-option>
        </a-select>
      </div>
    </div>

    <!-- 拖拽上传区域 -->
    <div class="upload-dropzone-box" @click="triggerUpload">
      <div class="cloud-icon-wrap">
        <svg viewBox="0 0 1024 1024" width="36" height="36" fill="#64748b">
          <path d="M544 640l-96-96h64V384h64v160h64z" />
          <path d="M811.4 366.7C765.6 245.9 648.9 160 512 160c-157.9 0-289.4 113.8-316.9 265.1C107.5 450.4 40 531.7 40 628.6 40 739.7 130.3 830 241.4 830h527.2c134.4 0 243.4-109 243.4-243.4 0-109.9-73.4-203.4-173.6-231.9H811.4zM768.6 766H241.4C165.6 766 104 704.4 104 628.6c0-68.5 50.1-125.7 116.3-135.2l26.1-3.7 5.7-25.7C275.1 361.3 384.6 288 512 288c116.8 0 218 61.8 247.6 158.4l9.5 30.9 31.9 6.2C859.9 494.9 904 547.4 904 606.6c0 88-71.4 159.4-159.4 159.4z" />
        </svg>
      </div>

      <div class="drop-text-main">将文件拖拽到此处，或点击上传</div>
      <div class="drop-text-sub">支持 CSV、XLS、XLSX 格式，单文件不超过 5 MB，最多 5000 条记录</div>

      <div class="dropzone-actions">
        <button class="btn-import-file" @click.stop="triggerUpload">
          <svg viewBox="0 0 1024 1024" width="14" height="14" fill="currentColor">
            <path d="M544 128H288c-35.3 0-64 28.7-64 64v640c0 35.3 28.7 64 64 64h448c35.3 0 64-28.7 64-64V384L544 128zm192 704H288V192h224v224h224v416z" />
            <path d="M512 704l-96-96h64V480h64v128h64z" />
          </svg>
          <span>导入交易文件</span>
        </button>

        <button class="btn-download-tpl" @click.stop="$emit('downloadTemplate')">
          <svg viewBox="0 0 1024 1024" width="14" height="14" fill="currentColor">
            <path d="M505.7 661a8 8 0 0 0 12.6 0l112-141.7c4.1-5.2.4-12.9-6.3-12.9h-74.1V168c0-4.4-3.6-8-8-8h-60c-4.4 0-8 3.6-8 8v338.3H400c-6.7 0-10.4 7.7-6.3 12.9l112 141.8zM878 626h-60c-4.4 0-8 3.6-8 8v154H214V634c0-4.4-3.6-8-8-8h-60c-4.4 0-8 3.6-8 8v198c0 17.7 14.3 32 32 32h684c17.7 0 32-14.3 32-32V634c0-4.4-3.6-8-8-8z" />
          </svg>
          <span>下载模板</span>
        </button>
      </div>
    </div>

    <!-- 隐藏的文件上传 input -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".csv,.xls,.xlsx"
      style="display: none;"
      @change="handleFileChange"
    />

    <!-- 底部说明提示 -->
    <div class="bottom-hint-row">
      <div class="info-icon">
        <svg viewBox="0 0 1024 1024" width="14" height="14" fill="#64748b">
          <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z" />
          <path d="M464 336a48 48 0 1 0 96 0 48 48 0 1 0-96 0zm72 176h-48c-4.4 0-8 3.6-8 8v272c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V520c0-4.4-3.6-8-8-8z" />
        </svg>
      </div>
      <span class="hint-text">{{ currentHintText }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { BrokerAccountVO } from '@/types/portfolio';

const props = defineProps<{
  accounts?: BrokerAccountVO[];
  selectedAccountId?: number;
}>();

const emit = defineEmits<{
  (e: 'fileSelected', file: File): void;
  (e: 'downloadTemplate'): void;
}>();

const selectedBroker = ref<string>('CMS');
const targetAccountId = ref<number | undefined>(props.selectedAccountId);

watch(
  () => props.selectedAccountId,
  (val) => {
    targetAccountId.value = val;
  }
);

watch(
  () => props.accounts,
  (accs) => {
    if (!targetAccountId.value && accs && accs.length > 0 && accs[0]) {
      targetAccountId.value = accs[0].id;
    }
  },
  { immediate: true }
);

const currentHintText = computed(() => {
  if (selectedBroker.value === 'CMS') {
    return '招商证券支持直接上传客户端导出的原始资金流水文件 (XLS/XLSX/CSV)。';
  }
  return '当前券商暂推荐使用 AQuant 标准模板格式导入，请下载模板并按规范整理后上传。';
});

const fileInputRef = ref<HTMLInputElement | null>(null);

const triggerUpload = () => {
  fileInputRef.value?.click();
};

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files.length > 0 && target.files[0]) {
    const file = target.files[0];
    emit('fileSelected', file);
    message.success(`已选择文件：${file.name}`);
    target.value = '';
  }
};
</script>

<style scoped>
.import-upload-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 14px;
}

.import-steps-vertical {
  background: #f8fafc;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
}

.v-step-item {
  display: flex;
  gap: 12px;
  position: relative;
}

.v-step-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  width: 22px;
  flex-shrink: 0;
}

.v-step-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  z-index: 2;
}

.v-step-num.is-active {
  background: #0f172a;
  color: #ffffff;
}

.v-step-line {
  position: absolute;
  top: 22px;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  background: #e2e8f0;
  z-index: 1;
}

.v-step-content {
  padding-bottom: 14px;
  display: flex;
  flex-direction: column;
}

.v-step-item:last-child .v-step-content {
  padding-bottom: 0;
}

.v-step-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  line-height: 22px; /* 与 22px 的圆圈高度垂直居中对齐 */
}

.v-step-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
  line-height: 1.4;
}

.import-config-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.config-item {
  flex: 1;
  min-width: 140px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-label {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
}

.step-text-wrap {
  display: flex;
  flex-direction: column;
}

.step-main-text {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.step-sub-text {
  font-size: 11px;
  color: #94a3b8;
}

.step-arrow-line {
  color: #cbd5e1;
  font-size: 14px;
}

.upload-dropzone-box {
  border: 1.5px dashed #e2e8f0;
  border-radius: 10px;
  padding: 32px 20px;
  text-align: center;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 14px;
}

.upload-dropzone-box:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
}

.cloud-icon-wrap {
  margin-bottom: 8px;
}

.drop-text-main {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.drop-text-sub {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  margin-bottom: 18px;
}

.dropzone-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.btn-import-file {
  background: #0f172a;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.15s;
}

.btn-import-file:hover {
  background: #1e293b;
}

.btn-download-tpl {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #0f172a;
  border-radius: 6px;
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s;
}

.btn-download-tpl:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.bottom-hint-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.hint-text {
  font-size: 12px;
  color: #64748b;
}
</style>
