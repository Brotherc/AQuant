<template>
  <div class="article-list-container">
    <a-card :bordered="false">
      <!-- 标题和操作栏 -->
      <div class="header-section" :class="{ 'justify-end': !isMyArticles }">
        <a-button v-if="isMyArticles" type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          创建文章
        </a-button>
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索文章标题或内容"
          style="width: 300px"
          @search="handleSearch"
          allow-clear
        />
      </div>

      <!-- 文章列表 -->
      <a-list
        :loading="loading"
        :data-source="articles"
        class="article-list"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta @click="handleView(item.id)" class="clickable-meta">
              <template #title>
                <span class="article-title">
                  {{ item.title }}
                </span>
              </template>
              <template #description>
                <!-- 显示摘要 -->
                <div v-if="item.summary" class="article-summary">
                  {{ item.summary }}
                </div>
                <!-- 元数据信息 -->
                <a-space :size="16" class="article-meta">
                  <span v-if="!isMyArticles">
                    <UserOutlined />
                    {{ item.authorUsername }}
                  </span>
                  <span>
                    <CalendarOutlined />
                    {{ formatDate(item.updatedAt !== item.createdAt ? item.updatedAt : item.createdAt) }}
                  </span>
                </a-space>
              </template>
            </a-list-item-meta>
            <template #actions v-if="isMyArticles">
              <a-select
                :value="item.visibility"
                @change="(value) => handleVisibilityChange(item.id, value)"
                size="small"
                style="width: 90px"
              >
                <a-select-option :value="0">
                  <LockOutlined /> 私密
                </a-select-option>
                <a-select-option :value="1">
                  <GlobalOutlined /> 公开
                </a-select-option>
              </a-select>
              <a @click="handleEdit(item)">编辑</a>
              <a-popconfirm
                title="确定要删除这篇文章吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(item.id)"
              >
                <a style="color: #ff4d4f;">删除</a>
              </a-popconfirm>
            </template>
          </a-list-item>
        </template>
      </a-list>

      <!-- 加载更多提示 -->
      <div v-if="hasMore && !loading" class="load-more-trigger" ref="loadMoreTrigger">
        <a-spin />
      </div>

      <!-- 没有更多数据提示 -->
      <div v-if="!hasMore && articles.length > 0" class="no-more-data">
        没有更多文章了
      </div>

      <!-- 空状态 -->
      <a-empty v-if="!loading && articles.length === 0" description="暂无文章" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { useRouter, useRoute } from 'vue-router';
import {
  PlusOutlined,
  CalendarOutlined,
  EditOutlined,
  UserOutlined,
  LockOutlined,
  GlobalOutlined
} from '@ant-design/icons-vue';
import {
  getPublicArticles,
  getUserArticles,
  deleteArticle,
  updateArticleVisibility,
  type ArticleListItemVO
} from '@/api/article';

// Props
const props = defineProps<{
  isMyArticles?: boolean;
}>();

// Router
const router = useRouter();
const route = useRoute();

// 状态
const loading = ref(false);
const articles = ref<ArticleListItemVO[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const hasMore = ref(true);
const searchKeyword = ref('');
const loadMoreTrigger = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

// 加载文章列表
const loadArticles = async (append = false) => {
  if (loading.value) return;
  
  loading.value = true;
  try {
    let response;
    
    // 统一使用同一个接口，通过 keyword 参数区分列表/搜索
    if (props.isMyArticles) {
      response = await getUserArticles({
        keyword: searchKeyword.value || undefined,
        page: currentPage.value,
        size: pageSize.value
      });
    } else {
      response = await getPublicArticles({
        keyword: searchKeyword.value || undefined,
        page: currentPage.value,
        size: pageSize.value
      });
    }

    if (response.data.success) {
      const newArticles = response.data.data.content;
      
      if (append) {
        // 追加模式：添加到现有列表
        articles.value = [...articles.value, ...newArticles];
      } else {
        // 替换模式：重置列表
        articles.value = newArticles;
      }
      
      // 判断是否还有更多数据
      hasMore.value = !response.data.data.last;
      
      // 如果有更多数据，设置观察器
      if (hasMore.value) {
        nextTick(() => {
          setupObserver();
        });
      }
    } else {
      message.error(response.data.message || '加载文章列表失败');
    }
  } catch (error) {
    console.error('加载文章列表失败:', error);
    message.error('加载文章列表失败');
  } finally {
    loading.value = false;
  }
};

// 加载更多
const loadMore = () => {
  if (!hasMore.value || loading.value) return;
  currentPage.value++;
  loadArticles(true);
};

// 设置 Intersection Observer
const setupObserver = () => {
  // 清理旧的观察器
  if (observer) {
    observer.disconnect();
  }
  
  // 创建新的观察器
  if (loadMoreTrigger.value) {
    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore.value && !loading.value) {
          loadMore();
        }
      },
      {
        rootMargin: '100px' // 提前100px触发加载
      }
    );
    
    observer.observe(loadMoreTrigger.value);
  }
};

// 搜索
const handleSearch = () => {
  currentPage.value = 1;
  hasMore.value = true;
  loadArticles(false);
};

// 查看文章详情
const handleView = (id: number) => {
  router.push({ name: 'ArticleDetail', params: { id } });
};

// 创建文章
const handleCreate = () => {
  router.push({ name: 'ArticleCreate' });
};

// 编辑文章
const handleEdit = (article: ArticleListItemVO) => {
  router.push({ name: 'ArticleEdit', params: { id: article.id } });
};

// 删除文章
const handleDelete = async (id: number) => {
  try {
    const response = await deleteArticle({ id });
    if (response.data.success) {
      message.success('删除成功');
      // 重新加载当前页
      currentPage.value = 1;
      hasMore.value = true;
      loadArticles(false);
    } else {
      message.error(response.data.message || '删除失败');
    }
  } catch (error) {
    console.error('删除文章失败:', error);
    message.error('删除失败');
  }
};

// 修改文章可见性
const handleVisibilityChange = async (articleId: number, newVisibility: number) => {
  try {
    // 调用更新可见性接口
    const response = await updateArticleVisibility({
      id: articleId,
      visibility: newVisibility
    });

    if (response.data.success) {
      // 更新本地数据
      const article = articles.value.find(a => a.id === articleId);
      if (article) {
        article.visibility = newVisibility;
      }
      message.success(`已修改为${newVisibility === 1 ? '公开' : '私密'}`);
    } else {
      message.error(response.data.message || '修改失败');
    }
  } catch (error) {
    console.error('修改可见性失败:', error);
    message.error('修改失败');
  }
};

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60));
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60));
      return minutes === 0 ? '刚刚' : `${minutes}分钟前`;
    }
    return `${hours}小时前`;
  } else if (days === 1) {
    return '昨天';
  } else if (days < 7) {
    return `${days}天前`;
  } else {
    return date.toLocaleDateString('zh-CN');
  }
};

// 初始化
onMounted(() => {
  loadArticles(false);
});

// 监听路由变化，重新加载数据
watch(() => route.path, () => {
  // 重置状态
  currentPage.value = 1;
  searchKeyword.value = '';
  hasMore.value = true;
  articles.value = [];
  // 重新加载文章列表
  loadArticles(false);
});

// 清理观察器
onUnmounted(() => {
  if (observer) {
    observer.disconnect();
  }
});
</script>

<style scoped>
.article-list-container {
  /* 移除额外的 padding，使用布局容器的 padding */
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-section.justify-end {
  justify-content: flex-end;
}

.article-list :deep(.ant-list-item) {
  padding: 16px 24px;
  transition: background-color 0.3s;
}

.article-list :deep(.ant-list-item:hover) {
  background-color: #fafafa;
}

.clickable-meta {
  cursor: pointer;
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  word-break: break-word;
  overflow-wrap: break-word;
}

:deep(.ant-list-item-meta-description) {
  color: #8c8c8c;
  font-size: 14px;
}

.article-summary {
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
  margin-top: 12px;
  margin-bottom: 12px;
  word-break: break-word;
  overflow-wrap: break-word;
}

.article-meta {
  margin-top: 8px;
}

:deep(.ant-list-item-action > li) {
  padding: 0 8px;
}

:deep(.ant-list-item-action a) {
  color: #1890ff;
}

.load-more-trigger {
  text-align: center;
  padding: 24px 0;
}

.no-more-data {
  text-align: center;
  padding: 24px 0;
  color: #8c8c8c;
  font-size: 14px;
}
</style>
