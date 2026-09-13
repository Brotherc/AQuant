<template>
  <a-modal
    :visible="visible"
    title="录入期初持仓"
    :confirm-loading="submitting"
    width="540px"
    centered
    wrap-class-name="init-form-modal-wrap"
    :body-style="{ maxHeight: 'calc(85vh - 110px)', overflowY: 'auto', padding: '16px 24px' }"
    ok-text="确认录入"
    cancel-text="取消"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
      class="init-form"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="所属券商账户" name="accountId">
            <a-select
              v-model:value="formState.accountId"
              placeholder="请选择券商账户"
            >
              <a-select-option
                v-for="acc in accounts"
                :key="acc.id"
                :value="acc.id"
              >
                {{ acc.accountName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item label="资产类型" name="assetType">
            <a-select v-model:value="formState.assetType">
              <a-select-option
                v-for="(label, key) in ASSET_TYPE_LABELS"
                :key="key"
                :value="key"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="标的代码" name="assetCode">
            <a-input
              v-model:value="formState.assetCode"
              placeholder="如 600519 / 00700"
              @change="formState.assetCode = (formState.assetCode || '').trim().toUpperCase()"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item label="标的名称" name="assetName">
            <a-input
              v-model:value="formState.assetName"
              placeholder="如 贵州茅台 (选填)"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="期初持仓数量" name="quantity">
            <a-input-number
              v-model:value="formState.quantity"
              :min="0.0001"
              :step="100"
              style="width: 100%;"
              placeholder="持仓股数/份额"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item label="持仓成本均价" name="costPrice">
            <a-input-number
              v-model:value="formState.costPrice"
              :min="0.0001"
              :step="0.01"
              style="width: 100%;"
              placeholder="持仓成本单价"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="期初建仓日期" name="positionTime">
            <a-date-picker
              v-model:value="formState.positionTime"
              value-format="YYYY-MM-DD[T]00:00:00"
              style="width: 100%;"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item label="币种" name="currency">
            <a-select v-model:value="formState.currency">
              <a-select-option value="CNY">人民币 (CNY)</a-select-option>
              <a-select-option value="USD">美元 (USD)</a-select-option>
              <a-select-option value="HKD">港币 (HKD)</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 预估期初总成本 -->
      <div class="cost-preview-box">
        <span class="preview-label">期初总成本：</span>
        <span class="preview-value">{{ estimatedCost }} {{ formState.currency }}</span>
      </div>

      <a-form-item label="备注" name="remark">
        <a-textarea
          v-model:value="formState.remark"
          placeholder="可填写期初建仓来源或说明（选填）"
          :rows="2"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import type { BrokerAccountVO, PositionInitRequest } from '@/types/portfolio';
import { ASSET_TYPE_LABELS } from '@/types/portfolio';
import { initPosition } from '@/api/portfolio';

const props = defineProps<{
  visible: boolean;
  portfolioId?: number;
  accounts: BrokerAccountVO[];
}>();

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const getToday = () => {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const formState = reactive<PositionInitRequest>({
  accountId: undefined as any,
  assetCode: '',
  assetName: '',
  assetType: 'STOCK',
  quantity: undefined as any,
  costPrice: undefined as any,
  positionTime: `${getToday()}T00:00:00`,
  currency: 'CNY',
  remark: ''
});

const rules = {
  accountId: [{ required: true, message: '请选择所属券商账户', trigger: 'change' }],
  assetType: [{ required: true, message: '请选择资产类型', trigger: 'change' }],
  assetCode: [{ required: true, message: '请输入标的代码', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入期初持仓数量', trigger: 'blur' }],
  costPrice: [{ required: true, message: '请输入持仓成本单价', trigger: 'blur' }],
  positionTime: [{ required: true, message: '请选择期初日期', trigger: 'change' }],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }]
};

const estimatedCost = computed(() => {
  if (formState.quantity && formState.costPrice) {
    const val = Number(formState.quantity) * Number(formState.costPrice);
    return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  return '0.00';
});

watch(
  () => props.visible,
  (val) => {
    if (val) {
      if (props.accounts.length > 0 && props.accounts[0] && !formState.accountId) {
        formState.accountId = props.accounts[0].id;
      }
      formState.positionTime = `${getToday()}T00:00:00`;
    }
  }
);

const handleCancel = () => {
  emit('update:visible', false);
  formRef.value?.resetFields();
};

const handleSubmit = async () => {
  if (!props.portfolioId) {
    message.warning('请先选择投资组合');
    return;
  }
  try {
    await formRef.value?.validate();
    submitting.value = true;
    await initPosition(props.portfolioId, formState);
    message.success('期初持仓录入成功');
    emit('update:visible', false);
    emit('success');
    formRef.value?.resetFields();
  } catch (err: any) {
    if (!err?.portfolioHandled && err?.message) {
      message.error(err.message);
    }
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.init-form {
  margin-top: 8px;
}

.cost-preview-box {
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  border: 1px solid #e2e8f0;
}

.preview-label {
  font-size: 13px;
  color: #64748b;
}

.preview-value {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #2563eb;
}

:global(.init-form-modal-wrap .ant-modal-footer) {
  border-top: none !important;
  padding: 10px 24px 20px 24px !important;
}
</style>
