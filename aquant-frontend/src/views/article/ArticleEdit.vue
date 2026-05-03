<template>
  <div class="article-edit-container">
    <a-card :loading="loading" :bordered="false">
      <!-- 顶部操作栏 -->
      <div class="header-actions">
        <a-button type="link" @click="handleBack" class="back-button">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <div class="action-buttons">
          <a-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
            <template #icon><SaveOutlined /></template>
            {{ isEditMode ? '保存' : '发布' }}
          </a-button>
        </div>
      </div>

      <div class="edit-content">
        <a-form
          :model="formData"
          :label-col="{ span: 2 }"
          :wrapper-col="{ span: 21 }"
        >
          <a-form-item
            label="标题"
            name="title"
            :rules="[
              { required: true, message: '请输入文章标题' },
              { max: 200, message: '标题不能超过200字符' }
            ]"
          >
            <a-input
              v-model:value="formData.title"
              placeholder="请输入文章标题"
              :maxlength="200"
              show-count
              size="large"
            />
          </a-form-item>

          <a-form-item
            label="内容"
            name="content"
            :rules="[
              { required: true, message: '请输入文章内容' },
              { max: 50000, message: '内容不能超过50000字符' }
            ]"
          >
            <TiptapEditor
              v-model="formData.content"
              placeholder="开始写作，支持 Markdown 格式..."
              :max-length="50000"
            />
          </a-form-item>

          <a-form-item
            v-if="!isEditMode"
            label="可见性"
            name="visibility"
            :rules="[{ required: true, message: '请选择可见性' }]"
          >
            <a-radio-group v-model:value="formData.visibility">
              <a-radio :value="0">
                <LockOutlined />
                私密（仅自己可见）
              </a-radio>
              <a-radio :value="1">
                <GlobalOutlined />
                公开（所有人可见）
              </a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  ArrowLeftOutlined,
  SaveOutlined,
  LockOutlined,
  GlobalOutlined
} from '@ant-design/icons-vue';
import {
  getArticleDetail,
  createArticle,
  updateArticle,
  type ArticleCreateReqVO,
  type ArticleUpdateReqVO
} from '@/api/article';
import TiptapEditor from '@/components/TiptapEditor.vue';

// Router
const route = useRoute();
const router = useRouter();

// 状态
const loading = ref(false);
const submitting = ref(false);
const formData = reactive<ArticleCreateReqVO & { id?: number }>({
  title: '',
  content: '',
  visibility: 0
});

// 计算属性
const isEditMode = computed(() => !!route.params.id);

// 加载文章详情（编辑模式）
const loadArticle = async () => {
  const articleId = Number(route.params.id);
  if (!articleId) return;

  loading.value = true;
  try {
    const response = await getArticleDetail(articleId);
    if (response.data.success) {
      const article = response.data.data;
      formData.id = article.id;
      formData.title = article.title;
      formData.content = article.content;
      formData.visibility = article.visibility;
    } else {
      message.error(response.data.message || '加载文章失败');
      handleBack();
    }
  } catch (error) {
    console.error('加载文章失败:', error);
    message.error('加载文章失败');
    handleBack();
  } finally {
    loading.value = false;
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formData.title.trim()) {
    message.warning('请输入文章标题');
    return;
  }
  if (!formData.content.trim()) {
    message.warning('请输入文章内容');
    return;
  }

  submitting.value = true;
  try {
    if (isEditMode.value && formData.id) {
      // 编辑模式
      const response = await updateArticle({
        id: formData.id,
        title: formData.title,
        content: formData.content,
        visibility: formData.visibility
      });
      if (response.data.success) {
        message.success('更新成功');
        router.push({ name: 'MyArticles' });
      } else {
        message.error(response.data.message || '更新失败');
      }
    } else {
      // 创建模式
      const response = await createArticle({
        title: formData.title,
        content: formData.content,
        visibility: formData.visibility
      });
      if (response.data.success) {
        message.success('发布成功');
        router.push({ name: 'MyArticles' });
      } else {
        message.error(response.data.message || '发布失败');
      }
    }
  } catch (error) {
    console.error('提交失败:', error);
    message.error('操作失败');
  } finally {
    submitting.value = false;
  }
};

// 返回
const handleBack = () => {
  router.back();
};

// 初始化
onMounted(() => {
  if (isEditMode.value) {
    loadArticle();
  }
});
</script>

<style scoped>
.article-edit-container {
  max-width: 1400px;
  margin: 0 auto;
}

.header-actions {
  display: grid;
  grid-template-columns: 8.333% 87.5% 4.167%; /* 2/24, 21/24, 1/24 to match form layout */
  align-items: center;
  margin-bottom: 24px;
}

.back-button {
  padding-left: 0;
  grid-column: 1;
}

.action-buttons {
  grid-column: 2;
  display: flex;
  justify-content: flex-end;
}

.edit-content {
  padding: 0;
}

:deep(.ant-form-item-label > label) {
  font-weight: 500;
}

:deep(.ant-input) {
  font-size: 15px;
}

:deep(.ant-radio-wrapper) {
  display: inline-flex;
  align-items: center;
  margin-right: 32px;
  margin-bottom: 0;
}

:deep(.ant-radio-group) {
  display: flex;
  align-items: center;
}
</style>
