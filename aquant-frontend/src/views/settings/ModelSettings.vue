<template>
  <div class="model-settings-page" :class="{ 'is-embedded': props.embedded }">
    <section v-if="!props.embedded" class="settings-hero">
      <div class="hero-left">
        <div class="settings-eyebrow">AI WORKSPACE</div>
        <h1>模型与供应商配置</h1>
        <p>统一管理各大 LLM 供应商的 API 接入点、密钥与模型能力矩阵，为量化投研与 AI 诊断提供智能动力。</p>
      </div>
      <div class="security-note">
        <safety-certificate-outlined class="security-icon" />
        <div class="security-text">
          <strong>密钥本地加密存储</strong>
          <span>安全隔离保护，页面绝不回显完整 API Key</span>
        </div>
      </div>
    </section>

    <section class="settings-workspace">
      <a-spin :spinning="loading">
        <div class="workspace-layout">
          <!-- 左侧供应商列表 -->
          <aside class="provider-panel">
            <div class="panel-heading">
              <div class="panel-title-group">
                <span class="panel-title">供应商</span>
                <span class="count-badge">{{ providers.length }} 个</span>
              </div>
              <a-button type="primary" size="small" class="add-provider-btn" @click="openProviderModal()">
                <plus-outlined />
                <span>新建</span>
              </a-button>
            </div>

            <div v-if="providers.length" class="provider-list" @scroll="closeProviderOverlays">
              <article
                v-for="provider in providers"
                :key="provider.id"
                class="provider-item"
                :class="{ 'is-active': provider.id === selectedProviderId }"
                @click="selectedProviderId = provider.id"
              >
                <div class="provider-card-inner">
                  <!-- 顶部信息：图标、名称与操作。 -->
                  <div class="provider-header">
                    <div class="provider-brand">
                      <div class="brand-avatar">
                        <provider-logo :type="provider.provider_type" :size="20" />
                      </div>
                      <div class="brand-info">
                        <div class="brand-name" :title="provider.name">{{ provider.name }}</div>
                      </div>
                    </div>

                    <div class="provider-header-right">
                      <span v-if="provider.id === defaultProviderId" class="provider-default-badge">
                        默认
                      </span>
                      <div class="provider-actions" @click.stop>
                        <a-dropdown
                          :trigger="['click']"
                          :open="openedProviderMenuId === provider.id"
                          placement="bottomRight"
                          @open-change="handleProviderMenuOpenChange(provider.id, $event)"
                        >
                          <button type="button" class="more-btn" aria-label="更多操作">
                            <more-outlined />
                          </button>
                          <template #overlay>
                            <a-menu class="custom-dropdown-menu">
                              <a-menu-item @click="openProviderModal(provider)">
                                编辑
                              </a-menu-item>
                              <a-menu-divider />
                              <a-menu-item danger @click.stop>
                                <div class="provider-delete-menu-item" @click.stop>
                                  <a-popconfirm
                                    :title="provider.model_count
                                      ? `确定删除该供应商及其下的 ${provider.model_count} 个模型？`
                                      : '确定删除该供应商？'"
                                    ok-text="确认删除"
                                    cancel-text="取消"
                                    :open="deleteConfirmProviderId === provider.id"
                                    placement="rightTop"
                                    @open-change="handleProviderDeleteConfirmOpenChange(provider.id, $event)"
                                    @confirm="removeProvider(provider)"
                                  >
                                    <div class="provider-delete-trigger">删除</div>
                                  </a-popconfirm>
                                </div>
                              </a-menu-item>
                            </a-menu>
                          </template>
                        </a-dropdown>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </div>

            <div v-else class="provider-empty">
              <a-empty description="暂无供应商" :image="simpleImage" />
            </div>
          </aside>

          <!-- 右侧模型矩阵列表 -->
          <main class="model-panel">
            <!-- 顶部操作栏 -->
            <div v-if="selectedProvider" class="model-toolbar">
              <div class="provider-connection-summary">
                <div class="connection-meta connection-meta-url">
                  <span class="connection-label">Base URL</span>
                  <a-tooltip :title="selectedProvider.base_url || '未配置'">
                    <code class="connection-value connection-url">
                      {{ selectedProvider.base_url || '未配置' }}
                    </code>
                  </a-tooltip>
                </div>
                <span class="connection-divider" aria-hidden="true"></span>
                <div class="connection-meta">
                  <span class="connection-label">API Key</span>
                  <span
                    class="connection-status-dot"
                    :class="{ 'is-configured': selectedProvider.has_api_key }"
                    aria-hidden="true"
                  ></span>
                  <code class="connection-value connection-key">
                    {{ selectedProvider.has_api_key ? (selectedProvider.api_key_hint || '已配置') : '未配置' }}
                  </code>
                </div>
              </div>
              <a-button
                type="primary"
                class="add-model-btn"
                @click="openModelModal()"
              >
                <plus-outlined />
                <span>添加模型</span>
              </a-button>
            </div>

            <!-- 空状态：未选择供应商 -->
            <div v-else class="model-empty-welcome">
              <a-empty description="请在左侧选择或添加供应商" />
            </div>

            <!-- 模型数据表格 -->
            <div v-if="selectedProvider" class="model-table-container">
              <a-table
                :columns="modelColumns"
                :data-source="selectedModels"
                :pagination="false"
                :scroll="{ y: 460 }"
                row-key="id"
                class="modern-model-table"
              >
                <template #bodyCell="{ column, record }">
                  <!-- 模型名称 -->
                  <template v-if="column.key === 'name'">
                    <span class="model-name-text">{{ record.name }}</span>
                  </template>

                  <!-- 模型标识 -->
                  <template v-else-if="column.key === 'modelName'">
                    <span class="model-code-tag">{{ record.model_name }}</span>
                  </template>

                  <!-- 连通性测试状态 -->
                  <template v-else-if="column.key === 'testStatus'">
                    <a-tooltip :title="record.last_test_message || (record.last_test_status === 'SUCCESS' ? '连通性正常' : '点击右侧「测试」发起检测')">
                      <span class="model-table-text">{{ statusLabel(record.last_test_status) }}</span>
                    </a-tooltip>
                  </template>

                  <!-- 默认模型 -->
                  <template v-else-if="column.key === 'isDefault'">
                    <a-tooltip :title="record.is_default ? '当前默认模型' : '设为默认模型'">
                      <span>
                        <a-switch
                          :checked="record.is_default"
                          :disabled="record.is_default"
                          size="small"
                          @change="handleDefaultChange(record, $event)"
                        />
                      </span>
                    </a-tooltip>
                  </template>

                  <!-- 操作列 -->
                  <template v-else-if="column.key === 'action'">
                    <div class="table-action-btns">
                      <a-button
                        type="link"
                        size="small"
                        class="action-btn action-test"
                        :loading="testingModelId === record.id"
                        @click="handleTest(record)"
                      >
                        <span>测试</span>
                      </a-button>

                      <a-button
                        type="link"
                        size="small"
                        class="action-btn action-edit"
                        @click="openModelModal(record)"
                      >
                        <span>编辑</span>
                      </a-button>

                      <a-popconfirm
                        title="确定删除此模型配置？"
                        ok-text="确认删除"
                        cancel-text="取消"
                        placement="topRight"
                        @confirm="removeModel(record)"
                      >
                        <a-button type="link" size="small" class="action-btn action-delete">
                          <span>删除</span>
                        </a-button>
                      </a-popconfirm>
                    </div>
                  </template>
                </template>

                <template #emptyText>
                  <div class="table-empty-box">
                    <a-empty description="当前供应商下尚未添加任何模型" :image="simpleImage" />
                  </div>
                </template>
              </a-table>
            </div>
          </main>
        </div>
      </a-spin>
    </section>

    <!-- 弹窗 1：供应商编辑/新增 -->
    <a-modal
      v-model:open="providerModalOpen"
      :title="editingProvider ? '编辑供应商' : '添加供应商'"
      :confirm-loading="providerSaving"
      width="540px"
      ok-text="保存"
      cancel-text="取消"
      centered
      class="custom-form-modal"
      @ok="saveProvider"
    >
      <a-form ref="providerFormRef" :model="providerForm" :rules="providerRules" layout="vertical" class="modern-form">
        <a-form-item label="配置名称" name="name">
          <a-input v-model:value="providerForm.name" placeholder="例如: 我的 DeepSeek 官方" />
        </a-form-item>

        <a-form-item label="供应商类型" name="provider_type">
          <a-select v-model:value="providerForm.provider_type" @change="handleProviderTypeChange">
            <a-select-option value="OPENAI">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo type="OPENAI" :size="16" />
                <span>OpenAI (官方标准)</span>
              </div>
            </a-select-option>
            <a-select-option value="DEEPSEEK">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo type="DEEPSEEK" :size="16" />
                <span>DeepSeek (深度求索)</span>
              </div>
            </a-select-option>
            <a-select-option value="QWEN">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo type="QWEN" :size="16" />
                <span>通义千问 (DashScope)</span>
              </div>
            </a-select-option>
            <a-select-option value="GEMINI">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo type="GEMINI" :size="16" />
                <span>Google Gemini (OpenAI 兼容端点)</span>
              </div>
            </a-select-option>
            <a-select-option value="MOONSHOT">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo type="MOONSHOT" :size="16" />
                <span>月之暗面 (Kimi / Moonshot)</span>
              </div>
            </a-select-option>
            <a-select-option value="ZHIPU">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo type="ZHIPU" :size="16" />
                <span>智谱 AI (GLM-4 系列)</span>
              </div>
            </a-select-option>
            <a-select-option value="OPENAI_COMPATIBLE">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo type="OPENAI_COMPATIBLE" :size="16" />
                <span>自定义 OpenAI 兼容接口</span>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="Base URL (接口接入地址)">
          <a-input
            :value="providerForm.base_url"
            readonly
            disabled
            placeholder="官方默认接入地址"
          />
        </a-form-item>

        <a-form-item label="API Key (接口密钥)" name="api_key">
          <a-input-password
            v-model:value="providerForm.api_key"
            :placeholder="editingProvider?.has_api_key ? '已配置密钥（留空表示保持原有密钥不变）' : '请输入 API 密钥'"
            autocomplete="new-password"
          />
          <div v-if="editingProvider?.api_key_hint" class="form-hint-info">
            当前生效密钥掩码：<code>{{ editingProvider.api_key_hint }}</code>
          </div>
        </a-form-item>

      </a-form>
    </a-modal>

    <!-- 弹窗 2：模型编辑/新增 -->
    <a-modal
      v-model:open="modelModalOpen"
      :title="editingModel ? '编辑模型' : '添加模型'"
      :confirm-loading="modelSaving"
      width="520px"
      ok-text="保存"
      cancel-text="取消"
      centered
      class="custom-form-modal"
      @ok="saveModel"
    >
      <a-form ref="modelFormRef" :model="modelForm" :rules="modelRules" layout="vertical" class="modern-form">
        <a-form-item label="所属供应商" name="provider_id">
          <a-select
            v-model:value="modelForm.provider_id"
            placeholder="请选择供应商"
            @change="handleModelProviderChange"
          >
            <a-select-option v-for="item in selectableProviders" :key="item.id" :value="item.id">
              <div style="display: flex; align-items: center; gap: 8px;">
                <provider-logo :type="item.provider_type" :size="16" />
                <span>{{ item.name }}</span>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="显示名称 (别名)" name="name">
          <a-input v-model:value="modelForm.name" placeholder="例如: DeepSeek V3 (主力)" />
        </a-form-item>

        <a-form-item label="模型" name="model_name">
          <div class="model-select-row">
            <a-select
              v-model:value="modelForm.model_name"
              :options="availableModelOptions"
              :loading="availableModelsLoading"
              :disabled="!modelForm.provider_id"
              show-search
              placeholder="请选择可用模型"
              option-filter-prop="label"
              class="model-identifier-select"
            >
              <template #notFoundContent>
                <a-spin v-if="availableModelsLoading" size="small" />
                <span v-else>该供应商未返回可用模型</span>
              </template>
            </a-select>
            <a-tooltip title="刷新模型列表">
              <a-button
                class="refresh-model-btn"
                :loading="availableModelsLoading"
                :disabled="!modelForm.provider_id"
                aria-label="刷新模型列表"
                @click="loadAvailableModels(modelForm.provider_id)"
              >
                <reload-outlined v-if="!availableModelsLoading" />
              </a-button>
            </a-tooltip>
          </div>
        </a-form-item>

      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { message, Empty, type FormInstance, type TableColumnsType } from 'ant-design-vue';
import {
  MoreOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue';
import ProviderLogo from '@/components/ProviderLogo.vue';
import {
  createAiModel,
  createModelProvider,
  deleteAiModel,
  deleteModelProvider,
  getAiModels,
  getAvailableAiModels,
  getModelProviders,
  setDefaultAiModel,
  testAiModel,
  updateAiModel,
  updateModelProvider,
  type AiModelConfig,
  type AiModelPayload,
  type ModelProvider,
  type ModelProviderPayload,
  type ProviderType
} from '@/api/aiModel';

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;

const props = withDefaults(defineProps<{
  embedded?: boolean;
}>(), {
  embedded: false
});

const providerDefaults: Record<ProviderType, string> = {
  OPENAI: 'https://api.openai.com/v1',
  DEEPSEEK: 'https://api.deepseek.com',
  QWEN: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  GEMINI: 'https://generativelanguage.googleapis.com/v1beta/openai/',
  MOONSHOT: 'https://api.moonshot.cn/v1',
  ZHIPU: 'https://open.bigmodel.cn/api/paas/v4',
  OPENAI_COMPATIBLE: 'https://api.openai.com/v1'
};

const loading = ref(false);
const providers = ref<ModelProvider[]>([]);
const models = ref<AiModelConfig[]>([]);
const selectedProviderId = ref<number>();
const testingModelId = ref<number>();
const openedProviderMenuId = ref<number>();
const deleteConfirmProviderId = ref<number>();

const providerModalOpen = ref(false);
const providerSaving = ref(false);
const providerFormRef = ref<FormInstance>();
const editingProvider = ref<ModelProvider>();
const providerForm = reactive<ModelProviderPayload>({
  name: '',
  provider_type: 'OPENAI',
  base_url: providerDefaults.OPENAI,
  api_key: '',
  clear_api_key: false
});

const closeProviderOverlays = () => {
  openedProviderMenuId.value = undefined;
  deleteConfirmProviderId.value = undefined;
};

const handleProviderMenuOpenChange = (providerId: number, open: boolean) => {
  if (!open && deleteConfirmProviderId.value === providerId) {
    return;
  }
  openedProviderMenuId.value = open ? providerId : undefined;
  if (!open) {
    deleteConfirmProviderId.value = undefined;
  }
};

const handleProviderDeleteConfirmOpenChange = (providerId: number, open: boolean) => {
  if (open) {
    openedProviderMenuId.value = providerId;
    deleteConfirmProviderId.value = providerId;
  } else {
    closeProviderOverlays();
  }
};

const modelModalOpen = ref(false);
const modelSaving = ref(false);
const availableModelsLoading = ref(false);
const availableModelNames = ref<string[]>([]);
let availableModelsRequestSequence = 0;
const modelFormRef = ref<FormInstance>();
const editingModel = ref<AiModelConfig>();
const modelForm = reactive<AiModelPayload>({
  provider_id: 0,
  name: '',
  model_name: ''
});

const providerRules = computed(() => ({
  name: [{ required: true, message: '请输入配置名称' }],
  provider_type: [{ required: true, message: '请选择供应商' }],
  api_key: [
    {
      validator: async (_rule: unknown, value: string) => {
        if (editingProvider.value?.has_api_key && (!value || !value.trim())) {
          return Promise.resolve();
        }
        if (!value || !value.trim()) {
          return Promise.reject(new Error('请输入 API 密钥'));
        }
        return Promise.resolve();
      },
      required: !editingProvider.value?.has_api_key
    }
  ]
}));

const modelRules = {
  provider_id: [{ required: true, type: 'number', min: 1, message: '请选择供应商' }],
  name: [{ required: true, message: '请输入显示名称' }],
  model_name: [{ required: true, message: '请选择模型' }]
};

const modelColumns: TableColumnsType<AiModelConfig> = [
  { title: '模型名称', key: 'name', width: 180 },
  { title: '模型标识', key: 'modelName', width: 200 },
  { title: '连通性', key: 'testStatus', width: 80 },
  { title: '默认模型', key: 'isDefault', width: 80 },
  { title: '操作', key: 'action', width: 170 }
];

const selectedProvider = computed(() => (
  providers.value.find((item) => item.id === selectedProviderId.value)
));
const defaultProviderId = computed(() => (
  models.value.find((item) => item.is_default)?.provider_id
));
const selectedModels = computed(() => (
  models.value.filter((item) => item.provider_id === selectedProviderId.value)
));
const selectableProviders = computed(() => providers.value);
const availableModelOptions = computed(() => availableModelNames.value.map((modelName) => ({
  label: modelName,
  value: modelName
})));

const loadData = async () => {
  loading.value = true;
  try {
    const [providerList, modelList] = await Promise.all([getModelProviders(), getAiModels()]);
    providers.value = providerList;
    models.value = modelList;
    if (!providerList.some((item) => item.id === selectedProviderId.value)) {
      selectedProviderId.value = providerList[0]?.id;
    }
  } finally {
    loading.value = false;
  }
};

const openProviderModal = (provider?: ModelProvider) => {
  editingProvider.value = provider;
  providerForm.name = provider?.name || '';
  providerForm.provider_type = provider?.provider_type || 'OPENAI';
  providerForm.base_url = provider?.base_url || providerDefaults[providerForm.provider_type];
  providerForm.api_key = '';
  providerForm.clear_api_key = false;
  providerModalOpen.value = true;
};

const handleProviderTypeChange = (type: ProviderType) => {
  providerForm.base_url = providerDefaults[type];
};

const saveProvider = async () => {
  await providerFormRef.value?.validate();
  providerSaving.value = true;
  try {
    const apiKeyTrimmed = (providerForm.api_key || '').trim();
    const payload: ModelProviderPayload = {
      name: (providerForm.name || '').trim(),
      provider_type: providerForm.provider_type,
      base_url: providerDefaults[providerForm.provider_type] || providerForm.base_url || '',
      clear_api_key: false
    };
    if (apiKeyTrimmed) {
      payload.api_key = apiKeyTrimmed;
    }
    let savedProvider: ModelProvider;
    if (editingProvider.value) {
      savedProvider = await updateModelProvider(editingProvider.value.id, payload);
    } else {
      savedProvider = await createModelProvider(payload);
    }
    selectedProviderId.value = savedProvider.id;
    providerModalOpen.value = false;
    message.success('供应商配置已保存');
    await loadData();
  } finally {
    providerSaving.value = false;
  }
};

const removeProvider = async (provider: ModelProvider) => {
  closeProviderOverlays();
  await deleteModelProvider(provider.id);
  message.success('供应商已删除');
  await loadData();
};

const loadAvailableModels = async (providerId: number) => {
  const requestSequence = ++availableModelsRequestSequence;
  if (!providerId) {
    availableModelNames.value = [];
    return;
  }
  availableModelsLoading.value = true;
  try {
    const modelNames = await getAvailableAiModels(providerId);
    if (requestSequence !== availableModelsRequestSequence) return;
    // 编辑已保存模型时，即使供应商暂时不再返回它，也保留当前值供用户确认。
    availableModelNames.value = modelForm.model_name && !modelNames.includes(modelForm.model_name)
      ? [modelForm.model_name, ...modelNames]
      : modelNames;
  } catch {
    if (requestSequence !== availableModelsRequestSequence) return;
    availableModelNames.value = modelForm.model_name ? [modelForm.model_name] : [];
  } finally {
    if (requestSequence === availableModelsRequestSequence) {
      availableModelsLoading.value = false;
    }
  }
};

const openModelModal = (model?: AiModelConfig) => {
  editingModel.value = model;
  modelForm.provider_id = model?.provider_id || selectedProvider.value?.id || providers.value[0]?.id || 0;
  modelForm.name = model?.name || '';
  modelForm.model_name = model?.model_name || '';
  availableModelNames.value = model?.model_name ? [model.model_name] : [];
  modelModalOpen.value = true;
  void loadAvailableModels(modelForm.provider_id);
};

const handleModelProviderChange = (providerId: number) => {
  modelForm.model_name = '';
  availableModelNames.value = [];
  void loadAvailableModels(providerId);
};

const saveModel = async () => {
  await modelFormRef.value?.validate();
  modelSaving.value = true;
  try {
    const payload = { ...modelForm };
    delete payload.api_mode;
    if (editingModel.value) {
      await updateAiModel(editingModel.value.id, payload);
    } else {
      await createAiModel(payload);
    }
    selectedProviderId.value = modelForm.provider_id;
    modelModalOpen.value = false;
    message.success('模型配置已成功保存');
    await loadData();
  } finally {
    modelSaving.value = false;
  }
};

const removeModel = async (model: AiModelConfig) => {
  await deleteAiModel(model.id);
  message.success('模型已删除');
  await loadData();
};

const handleDefaultChange = async (model: AiModelConfig, checked: boolean | string | number) => {
  if (checked !== true || model.is_default) return;
  await setDefaultAiModel(model.id);
  message.success(`已将「${model.name}」设为默认模型`);
  await loadData();
};

const handleTest = async (model: AiModelConfig) => {
  testingModelId.value = model.id;
  try {
    const result = await testAiModel(model.id);
    if (result.success) {
      message.success(`「${model.name}」连接正常，耗时 ${result.latency_ms}ms`);
    } else {
      message.error(result.message || '连接测试失败，请检查 Base URL 与 API Key');
    }
    await loadData();
  } finally {
    testingModelId.value = undefined;
  }
};

const statusLabel = (status?: string) => status === 'SUCCESS' ? '可用' : status === 'FAILED' ? '异常' : '未测试';

onMounted(() => {
  void loadData();
  document.addEventListener('wheel', closeProviderOverlays, { capture: true, passive: true });
  document.addEventListener('scroll', closeProviderOverlays, true);
});

onBeforeUnmount(() => {
  document.removeEventListener('wheel', closeProviderOverlays, true);
  document.removeEventListener('scroll', closeProviderOverlays, true);
});
</script>

<style scoped>
.model-settings-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: #1e293b;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.settings-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background:
    radial-gradient(circle at 90% 15%, rgba(37, 99, 235, 0.04), transparent 40%),
    linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.settings-eyebrow {
  margin-bottom: 6px;
  color: #3b82f6;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.15em;
}

.hero-left h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.hero-left p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  max-width: 680px;
}

.security-note {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.security-icon {
  color: #10b981;
  font-size: 22px;
}

.security-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.security-text strong {
  font-size: 12px;
  color: #0f172a;
}

.security-text span {
  font-size: 11px;
  color: #64748b;
}

/* 工作区布局 */
.settings-workspace {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.model-settings-page.is-embedded {
  padding: 16px;
}

.model-settings-page.is-embedded .settings-workspace {
  height: min(560px, calc(90vh - 97px));
  min-height: 0;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  background: transparent;
}

.model-settings-page.is-embedded .settings-workspace :deep(.ant-spin-nested-loading),
.model-settings-page.is-embedded .settings-workspace :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.model-settings-page.is-embedded .workspace-layout {
  height: 100%;
  min-height: 0;
}

.workspace-layout {
  display: grid;
  min-height: 560px;
  grid-template-columns: 290px minmax(0, 1fr);
  gap: 16px;
}

/* 左侧供应商面板 */
.provider-panel {
  min-width: 0;
  min-height: 0;
  height: 100%;
  box-sizing: border-box;
  overflow: hidden;
  padding: 16px 14px;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  margin: 0;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 28px;
  margin-bottom: 12px;
  padding: 0 4px;
}

.panel-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.count-badge {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  background: #e2e8f0;
  padding: 1px 7px;
  border-radius: 10px;
}

.add-provider-btn {
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #0f172a;
  border-color: #0f172a;
  box-shadow: none !important;
}

.add-provider-btn:hover {
  background: #1e293b !important;
  border-color: #1e293b !important;
  box-shadow: none !important;
}

.provider-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 2px 2px 0;
  scrollbar-width: thin;
}

.provider-item {
  position: relative;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.provider-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  transform: translateY(-1px);
}

.provider-item.is-active {
  border-color: #0f172a;
  background: #ffffff;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.provider-card-inner {
  padding: 10px 12px;
}

.provider-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.provider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.brand-avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  color: #0f172a;
  background: transparent;
  border: none;
  box-shadow: none;
  flex-shrink: 0;
}

.brand-info {
  min-width: 0;
  flex: 1;
}

.brand-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.provider-default-badge {
  padding: 2px 7px;
  border: 1px solid #fde68a;
  border-radius: 10px;
  background: #fef3c7;
  color: #b45309;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.2;
}

.provider-actions {
  display: flex;
  align-items: center;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.provider-item:hover .provider-actions {
  opacity: 1;
}

.more-btn {
  background: transparent;
  border: 0;
  color: #64748b;
  cursor: pointer;
  padding: 3px;
  border-radius: 4px;
  display: grid;
  place-items: center;
}

.more-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.provider-delete-menu-item {
  margin: -5px -12px;
  padding: 5px 12px;
}

.provider-delete-trigger {
  width: 100%;
}

.provider-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  min-height: 240px;
}

/* 右侧模型面板 */
.model-panel {
  min-width: 0;
  min-height: 0;
  height: 100%;
  box-sizing: border-box;
  padding: 16px 0 0;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

/* 右侧顶部操作工具栏 */
.model-toolbar {
  flex: 0 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 28px;
  margin-bottom: 12px;
}

.provider-connection-summary {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
}

.connection-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  white-space: nowrap;
}

.connection-meta-url {
  flex: 1;
}

.connection-label {
  flex-shrink: 0;
  color: #475569;
  font-weight: 600;
}

.connection-value {
  min-width: 0;
  padding: 3px 7px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #f8fafc;
  color: #334155;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.connection-url {
  display: block;
  max-width: 360px;
}

.connection-key {
  flex-shrink: 0;
}

.connection-divider {
  width: 1px;
  height: 16px;
  flex-shrink: 0;
  background: #e2e8f0;
}

.connection-status-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #cbd5e1;
}

.connection-status-dot.is-configured {
  background: #10b981;
}

.add-model-btn {
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #0f172a;
  border-color: #0f172a;
  box-shadow: none !important;
}

.add-model-btn:hover {
  background: #1e293b !important;
  border-color: #1e293b !important;
  box-shadow: none !important;
}

.model-empty-welcome {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  min-height: 360px;
}

/* 模型表格 */
.model-table-container {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.modern-model-table {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.modern-model-table :deep(.ant-table-wrapper),
.modern-model-table :deep(.ant-spin-nested-loading),
.modern-model-table :deep(.ant-spin-container),
.modern-model-table :deep(.ant-table),
.modern-model-table :deep(.ant-table-container) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.modern-model-table :deep(.ant-table-body) {
  flex: 1;
  min-height: 0;
  max-height: none !important;
  overflow-y: auto !important;
}

.modern-model-table :deep(.ant-table-placeholder) {
  height: 100%;
}

.modern-model-table :deep(.ant-table-placeholder > .ant-table-cell) {
  vertical-align: middle !important;
  text-align: center;
  padding: 0 !important;
}

.modern-model-table :deep(.ant-table-thead > tr > th) {
  background: #f1f5f9 !important;
  color: #334155 !important;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap !important;
}

.modern-model-table :deep(.ant-table-tbody > tr > td) {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s;
}

.modern-model-table :deep(.ant-table-tbody > tr:hover > td),
.modern-model-table :deep(.ant-table-tbody > tr > td.ant-table-cell-row-hover) {
  background: #fafcff !important;
  transition: none !important;
}

/* 表格单元格定制 */
.model-name-text {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.model-code-tag {
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
  color: #64748b;
}

.model-table-text {
  color: #475569;
  font-size: 12px;
  font-weight: 400;
}

/* 表格操作按钮 */
.table-action-btns {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

.action-btn {
  padding: 0 6px !important;
  font-size: 12px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.action-test {
  color: #2563eb !important;
}

.action-edit {
  color: #475569 !important;
}

.action-delete {
  color: #ef4444 !important;
}

.table-empty-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  min-height: 380px;
  width: 100%;
}

/* 弹窗表单通用美化 */
.modal-form-intro {
  margin-top: -8px;
  margin-bottom: 20px;
  font-size: 12px;
  color: #64748b;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-hint-info {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.form-hint-info code {
  background: #f1f5f9;
  padding: 1px 4px;
  border-radius: 4px;
  font-family: monospace;
}

.model-select-row {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
}

.model-identifier-select {
  min-width: 0;
  flex: 1;
}

.refresh-model-btn {
  flex: 0 0 32px;
  width: 32px;
  padding: 0 !important;
  border: 0 !important;
  box-shadow: none !important;
}

.refresh-model-btn:hover,
.refresh-model-btn:focus,
.refresh-model-btn:active {
  background: transparent !important;
}

.clear-key-wrapper {
  margin-top: 8px;
}

@media (max-width: 960px) {
  .workspace-layout {
    grid-template-columns: 1fr;
  }
  .provider-panel {
    border-right: 0;
    border-bottom: 1px solid #f1f5f9;
  }
  .provider-list {
    max-height: 240px;
  }
}
</style>
