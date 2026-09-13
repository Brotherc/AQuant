<template>
  <a-modal
    :visible="visible"
    :title="modalTitle"
    :confirm-loading="submitting"
    width="620px"
    centered
    wrap-class-name="trade-form-modal-wrap"
    :body-style="{ maxHeight: 'calc(85vh - 110px)', overflowY: 'auto', padding: '16px 24px' }"
    ok-text="确认提交"
    cancel-text="取消"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
      class="trade-form"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="所属券商账户" name="accountId">
            <a-select
              v-model:value="formState.accountId"
              placeholder="请选择账户"
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
          <a-form-item label="交易类型" name="tradeType">
            <a-select v-model:value="formState.tradeType" @change="handleTradeTypeChange">
              <a-select-option
                v-for="(label, key) in TRADE_TYPE_LABELS"
                :key="key"
                :value="key"
              >
                {{ label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 证券相关字段 (非纯资金存取) -->
      <template v-if="isSecurityRelated">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="标的代码" name="assetCode">
              <a-input
                v-model:value="formState.assetCode"
                placeholder="如 600519 / 000001"
                @change="formState.assetCode = (formState.assetCode || '').trim().toUpperCase()"
              />
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
            <a-form-item label="标的名称" name="assetName">
              <a-input
                v-model:value="formState.assetName"
                placeholder="如 贵州茅台 (选填)"
              />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="结算币种" name="currency">
              <a-select v-model:value="formState.currency">
                <a-select-option value="CNY">人民币 (CNY)</a-select-option>
                <a-select-option value="USD">美元 (USD)</a-select-option>
                <a-select-option value="HKD">港币 (HKD)</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </template>

      <!-- 价格与数量 -->
      <a-row :gutter="16">
        <a-col v-if="needPrice" :span="12">
          <a-form-item label="成交价格" name="price">
            <a-input-number
              v-model:value="formState.price"
              :min="0.0001"
              :step="0.01"
              style="width: 100%;"
              placeholder="单价"
              @change="calcAmount"
            />
          </a-form-item>
        </a-col>

        <a-col v-if="needQuantity" :span="12">
          <a-form-item label="数量 (股/份)" name="quantity">
            <a-input-number
              v-model:value="formState.quantity"
              :min="0.0001"
              :step="100"
              style="width: 100%;"
              placeholder="数量"
              @change="calcAmount"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 发生金额与交易日期 -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="发生金额" name="grossAmount">
            <a-input-number
              v-model:value="formState.grossAmount"
              :step="100"
              style="width: 100%;"
              placeholder="自动计算或手动微调"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item label="交易日期" name="tradeTime">
            <a-date-picker
              v-model:value="formState.tradeTime"
              value-format="YYYY-MM-DD[T]00:00:00"
              style="width: 100%;"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 交易费用折叠面板 (可选展开) -->
      <a-collapse ghost style="margin-bottom: 12px;">
        <a-collapse-panel key="fees" header="税费明细（佣金/印花税/过户费等，选填）">
          <a-row :gutter="12">
            <a-col :span="6">
              <a-form-item label="佣金" name="commission">
                <a-input-number v-model:value="formState.commission" :min="0" :step="1" style="width: 100%;" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="印花税" name="stampDuty">
                <a-input-number v-model:value="formState.stampDuty" :min="0" :step="1" style="width: 100%;" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="过户费" name="transferFee">
                <a-input-number v-model:value="formState.transferFee" :min="0" :step="1" style="width: 100%;" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="其他费用" name="otherFee">
                <a-input-number v-model:value="formState.otherFee" :min="0" :step="1" style="width: 100%;" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>

      <a-form-item label="备注说明" name="remark">
        <a-input v-model:value="formState.remark" placeholder="流水备注信息 (选填)" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import type {
  BrokerAccountVO,
  TradeType,
  AssetType,
  TradeRecordRequest
} from '@/types/portfolio';
import {
  TRADE_TYPE_LABELS,
  ASSET_TYPE_LABELS
} from '@/types/portfolio';
import { recordTrade } from '@/api/portfolio';

const props = defineProps<{
  visible: boolean;
  portfolioId?: number;
  accounts: BrokerAccountVO[];
  initialData?: {
    accountId?: number;
    assetCode?: string;
    assetName?: string;
    assetType?: AssetType;
    tradeType?: TradeType;
  };
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

const formState = reactive<TradeRecordRequest>({
  accountId: undefined as any,
  tradeType: 'BUY',
  assetCode: '',
  assetName: '',
  assetType: 'STOCK',
  price: undefined,
  quantity: undefined,
  grossAmount: undefined,
  tradeTime: `${getToday()}T00:00:00`,
  commission: undefined,
  stampDuty: undefined,
  transferFee: undefined,
  otherFee: undefined,
  currency: 'CNY',
  remark: ''
});

const isSecurityRelated = computed(() => {
  return !['DIVIDEND_CASH', 'FEE', 'TAX', 'INTEREST', 'CASH_DEPOSIT', 'CASH_WITHDRAW'].includes(formState.tradeType);
});

const needPrice = computed(() => {
  return ['BUY', 'SELL', 'SUBSCRIBE', 'REDEEM', 'TRANSFER_IN', 'TRANSFER_OUT'].includes(formState.tradeType);
});

const needQuantity = computed(() => {
  return ['BUY', 'SELL', 'SUBSCRIBE', 'REDEEM', 'TRANSFER_IN', 'TRANSFER_OUT', 'DIVIDEND_SHARE', 'POSITION_INIT'].includes(formState.tradeType);
});

const modalTitle = computed(() => {
  const label = TRADE_TYPE_LABELS[formState.tradeType] || '交易流水';
  return `记一笔 - ${label}`;
});

const rules = computed(() => {
  const baseRules: Record<string, any[]> = {
    accountId: [{ required: true, message: '请选择账户', trigger: 'change' }],
    tradeType: [{ required: true, message: '请选择交易类型', trigger: 'change' }],
    tradeTime: [{ required: true, message: '请选择交易日期', trigger: 'change' }],
    grossAmount: [{ required: true, message: '请输入发生金额', trigger: 'blur' }]
  };

  if (isSecurityRelated.value) {
    baseRules.assetCode = [{ required: true, message: '请输入标的代码', trigger: 'blur' }];
    baseRules.assetType = [{ required: true, message: '请选择资产类型', trigger: 'change' }];
  }

  if (needPrice.value) {
    baseRules.price = [{ required: true, message: '请输入成交单价', trigger: 'blur' }];
  }

  if (needQuantity.value) {
    baseRules.quantity = [{ required: true, message: '请输入数量', trigger: 'blur' }];
  }

  return baseRules;
});

const calcAmount = () => {
  if (formState.price !== undefined && formState.price !== null && formState.quantity) {
    const val = Number(formState.price) * Number(formState.quantity);
    formState.grossAmount = Number(val.toFixed(2));
  }
};

const handleTradeTypeChange = () => {
  if (!isSecurityRelated.value) {
    formState.assetCode = undefined;
    formState.assetName = undefined;
    formState.price = undefined;
    formState.quantity = undefined;
  }
};

watch(
  () => props.visible,
  (val) => {
    if (val) {
      formState.tradeTime = `${getToday()}T00:00:00`;

      if (props.initialData) {
        if (props.initialData.accountId) formState.accountId = props.initialData.accountId;
        else if (props.accounts.length > 0 && props.accounts[0] && !formState.accountId) formState.accountId = props.accounts[0].id;
        
        if (props.initialData.tradeType) formState.tradeType = props.initialData.tradeType;
        if (props.initialData.assetCode) formState.assetCode = props.initialData.assetCode;
        if (props.initialData.assetName) formState.assetName = props.initialData.assetName;
        if (props.initialData.assetType) formState.assetType = props.initialData.assetType;
      } else if (props.accounts.length > 0 && props.accounts[0] && !formState.accountId) {
        formState.accountId = props.accounts[0].id;
      }
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
    await recordTrade(formState);
    message.success('交易流水已成功记录');
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
.trade-form {
  margin-top: 4px;
}

.trade-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.trade-form :deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

.trade-form :deep(.ant-collapse-header) {
  padding: 8px 0 !important;
  font-size: 13px;
  color: #64748b;
}

.trade-form :deep(.ant-collapse-content-box) {
  padding: 8px 0 0 0 !important;
}

:global(.trade-form-modal-wrap .ant-modal-footer) {
  border-top: none !important;
  padding: 10px 24px 20px 24px !important;
}
</style>
