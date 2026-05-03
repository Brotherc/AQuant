<template>
  <div class="article-detail-container">
    <a-card :loading="loading" :bordered="false">
      <!-- 返回按钮 -->
      <a-button type="link" @click="handleBack" class="back-button">
        <template #icon><ArrowLeftOutlined /></template>
        返回列表
      </a-button>

      <div v-if="article" class="article-content">
        <!-- 文章头部 -->
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <a-space :size="16">
              <span>
                <UserOutlined />
                {{ article.authorUsername }}
              </span>
              <span>
                <CalendarOutlined />
                发布于 {{ formatDateTime(article.createdAt) }}
              </span>
              <span v-if="article.updatedAt !== article.createdAt">
                <EditOutlined />
                更新于 {{ formatDateTime(article.updatedAt) }}
              </span>
              <a-tag :color="article.visibility === 1 ? 'green' : 'orange'">
                {{ article.visibility === 1 ? '公开' : '私密' }}
              </a-tag>
            </a-space>
          </div>
        </div>

        <a-divider />

        <!-- 文章内容 -->
        <div class="article-body">
          <div class="article-text" v-html="article.content"></div>
        </div>

        <!-- 操作按钮（仅作者可见） -->
        <div v-if="isAuthor" class="article-actions">
          <a-space>
            <a-button type="primary" @click="handleEdit">
              <template #icon><EditOutlined /></template>
              编辑文章
            </a-button>
            <a-popconfirm
              title="确定要删除这篇文章吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete"
            >
              <a-button danger>
                <template #icon><DeleteOutlined /></template>
                删除文章
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </div>

      <!-- 错误提示 -->
      <a-result
        v-else-if="!loading && error"
        status="error"
        :title="error"
        :sub-title="errorDetail"
      >
        <template #extra>
          <a-button type="primary" @click="handleBack">
            返回列表
          </a-button>
        </template>
      </a-result>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  ArrowLeftOutlined,
  UserOutlined,
  CalendarOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue';
import { getArticleDetail, deleteArticle, type ArticleDetailVO } from '@/api/article';

// Router
const route = useRoute();
const router = useRouter();

// 状态
const loading = ref(false);
const article = ref<ArticleDetailVO | null>(null);
const error = ref('');
const errorDetail = ref('');

// 计算属性
const isAuthor = computed(() => {
  // TODO: 从用户状态中获取当前用户ID进行比较
  // 这里暂时返回false，需要集成用户认证状态
  return false;
});

// 加载文章详情
const loadArticle = async () => {
  const articleId = Number(route.params.id);
  if (!articleId) {
    error.value = '文章ID无效';
    return;
  }

  loading.value = true;
  try {
    const response = await getArticleDetail(articleId);
    if (response.data.success) {
      article.value = response.data.data;
    } else {
      error.value = '加载失败';
      errorDetail.value = response.data.message || '无法加载文章详情';
      message.error(response.data.message || '加载文章详情失败');
    }
  } catch (err: any) {
    console.error('加载文章详情失败:', err);
    error.value = '加载失败';
    if (err.response?.status === 403) {
      errorDetail.value = '您没有权限查看此文章';
    } else if (err.response?.status === 404) {
      errorDetail.value = '文章不存在';
    } else {
      errorDetail.value = '网络错误，请稍后重试';
    }
    message.error(errorDetail.value);
  } finally {
    loading.value = false;
  }
};

// 返回列表
const handleBack = () => {
  router.back();
};

// 编辑文章
const handleEdit = () => {
  if (article.value) {
    router.push({ name: 'ArticleEdit', params: { id: article.value.id } });
  }
};

// 删除文章
const handleDelete = async () => {
  if (!article.value) return;

  try {
    const response = await deleteArticle({ id: article.value.id });
    if (response.data.success) {
      message.success('删除成功');
      router.push({ name: 'MyArticles' });
    } else {
      message.error(response.data.message || '删除失败');
    }
  } catch (error) {
    console.error('删除文章失败:', error);
    message.error('删除失败');
  }
};

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 初始化
onMounted(() => {
  loadArticle();
});
</script>

<style scoped>
.article-detail-container {
  max-width: 1400px;
  margin: 0 auto;
}

.back-button {
  margin-bottom: 16px;
  padding-left: 0;
}

.article-content {
  padding: 24px 0;
}

.article-header {
  margin-bottom: 24px;
}

.article-title {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 16px;
  color: #262626;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.article-meta {
  color: #8c8c8c;
  font-size: 14px;
}

.article-body {
  margin: 32px 0;
  min-height: 300px;
}

.article-text {
  font-size: 16px;
  line-height: 1.8;
  color: #262626;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
  overflow-x: auto;
}

/* Tiptap 内容样式 */
.article-text :deep(h1) {
  font-size: 2em;
  font-weight: 700;
  margin: 0.67em 0;
  line-height: 1.3;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.article-text :deep(h2) {
  font-size: 1.5em;
  font-weight: 600;
  margin: 0.75em 0;
  line-height: 1.4;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.article-text :deep(h3) {
  font-size: 1.17em;
  font-weight: 600;
  margin: 0.83em 0;
  line-height: 1.5;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.article-text :deep(p) {
  margin: 1em 0;
  line-height: 1.8;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.article-text :deep(ul),
.article-text :deep(ol) {
  padding-left: 2em;
  margin: 1em 0;
}

.article-text :deep(li) {
  margin: 0.5em 0;
}

.article-text :deep(code) {
  background-color: #f5f5f5;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.article-text :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
  max-width: 100%;
  word-wrap: normal;
  white-space: pre-wrap;
}

.article-text :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
  font-size: 0.9em;
  word-wrap: normal;
  white-space: pre-wrap;
}

.article-text :deep(blockquote) {
  border-left: 4px solid #1890ff;
  padding-left: 1em;
  margin: 1em 0;
  color: #595959;
  font-style: italic;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
}

.article-text :deep(a) {
  color: #1890ff;
  text-decoration: underline;
  word-wrap: break-word;
  word-break: break-all;
  overflow-wrap: break-word;
}

.article-text :deep(a:hover) {
  color: #40a9ff;
}

.article-text :deep(hr) {
  border: none;
  border-top: 2px solid #f0f0f0;
  margin: 2em 0;
}

.article-text :deep(strong) {
  font-weight: 700;
}

.article-text :deep(em) {
  font-style: italic;
}

.article-text :deep(s) {
  text-decoration: line-through;
}

.article-actions {
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

:deep(.ant-divider) {
  margin: 24px 0;
}
</style>
