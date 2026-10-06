import aiRequest from '@/utils/aiRequest';

export type ProviderType =
  | 'OPENAI'
  | 'DEEPSEEK'
  | 'QWEN'
  | 'GEMINI'
  | 'MOONSHOT'
  | 'ZHIPU'
  | 'OPENAI_COMPATIBLE';
export type ApiMode = 'RESPONSES' | 'CHAT_COMPLETIONS';

/** 模型供应商连接，API Key 只暴露是否存在及末四位提示。 */
export interface ModelProvider {
  id: number;
  name: string;
  provider_type: ProviderType;
  base_url?: string;
  has_api_key: boolean;
  api_key_hint?: string;
  enabled: boolean;
  model_count: number;
  created_at: string;
  updated_at: string;
}

/** 创建或更新供应商时提交的数据。 */
export interface ModelProviderPayload {
  name: string;
  provider_type: ProviderType;
  base_url?: string;
  api_key?: string;
  clear_api_key?: boolean;
}

/** 可供 Agent Workflow 使用的模型及能力声明。 */
export interface AiModelConfig {
  id: number;
  provider_id: number;
  provider_name: string;
  provider_type: ProviderType;
  name: string;
  model_name: string;
  api_mode: ApiMode;
  supports_tool_calling: boolean;
  supports_structured_output: boolean;
  supports_streaming: boolean;
  supports_reasoning: boolean;
  request_timeout_seconds: number;
  extra_settings: Record<string, unknown>;
  is_default: boolean;
  enabled: boolean;
  last_test_status?: 'SUCCESS' | 'FAILED';
  last_test_message?: string;
  last_test_at?: string;
  created_at: string;
  updated_at: string;
}

/** 创建或更新模型时提交的数据。 */
export interface AiModelPayload {
  provider_id: number;
  name: string;
  model_name: string;
  api_mode?: ApiMode;
  is_default?: boolean;
}

/** 最小文本请求的连接测试结果。 */
export interface ModelTestResult {
  success: boolean;
  message: string;
  latency_ms: number;
}

export const getModelProviders = async () =>
  (await aiRequest.get<ModelProvider[]>('/modelConfig/provider/list')).data;

export const createModelProvider = async (data: ModelProviderPayload) =>
  (await aiRequest.post<ModelProvider>('/modelConfig/provider/create', data)).data;

export const updateModelProvider = async (id: number, data: Partial<ModelProviderPayload>) =>
  (await aiRequest.post<ModelProvider>('/modelConfig/provider/update', { id, ...data })).data;

export const deleteModelProvider = async (id: number) =>
  aiRequest.post('/modelConfig/provider/delete', { id });

export const getAiModels = async () =>
  (await aiRequest.get<AiModelConfig[]>('/modelConfig/model/list')).data;

/** 从供应商实时获取当前 API Key 有权使用的模型标识。 */
export const getAvailableAiModels = async (providerId: number) =>
  (await aiRequest.get<string[]>('/modelConfig/model/available', {
    params: { providerId },
    timeout: 30000
  })).data;

export const createAiModel = async (data: AiModelPayload) =>
  (await aiRequest.post<AiModelConfig>('/modelConfig/model/create', data)).data;

export const updateAiModel = async (id: number, data: Partial<AiModelPayload>) =>
  (await aiRequest.post<AiModelConfig>('/modelConfig/model/update', { id, ...data })).data;

export const deleteAiModel = async (id: number) =>
  aiRequest.post('/modelConfig/model/delete', { id });

export const setDefaultAiModel = async (id: number) =>
  (await aiRequest.post<AiModelConfig>('/modelConfig/model/setDefault', { id })).data;

export const testAiModel = async (id: number) =>
  (await aiRequest.post<ModelTestResult>('/modelConfig/model/test', { id })).data;
