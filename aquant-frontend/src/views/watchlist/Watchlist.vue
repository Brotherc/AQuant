<template>
  <div class="watchlist-container">
    <a-tabs v-model:activeKey="activeGroupId" @change="handleTabChange" type="editable-card" @edit="onTabEdit" hide-add>
      <template #tabBarExtraContent>
        <a-button type="primary" @click="showAddGroupModal">新增分组</a-button>
      </template>
        <a-tab-pane v-for="group in groups" :key="group.id" :tab="group.name" :closable="true">
          <div style="margin-bottom: 16px; display: flex; gap: 8px">
            <a-input-search
              v-model:value="newStockCode"
              placeholder="输入股票代码"
              enter-button="添加股票"
              @search="handleAddStock"
              style="width: 250px"
              :loading="addLoading"
            />
          </div>

          <a-table
            :columns="columns"
            :data-source="stocks"
            :loading="loading"
            row-key="stockCode"
            :pagination="false"
            :customRow="customRow"
          >
            <template #bodyCell="{ column, record, text }">
              <template v-if="column.dataIndex === 'changePercent'">
                <span :style="{ color: text > 0 ? '#ff4d4f' : text < 0 ? '#52c41a' : '#000' }">
                  {{ text > 0 ? '+' : '' }}{{ text != null ? text.toFixed(2) + '%' : '-' }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'latestPrice'">
                {{ text != null ? text.toFixed(2) : '-' }}
              </template>
              <template v-if="column.key === 'operation'">
                <a-popconfirm title="确定移除该股票吗？" @confirm="handleRemoveStock(record.stockCode)">
                  <a style="color: #ff4d4f">移除</a>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>

    <!-- 新增分组 Modal -->
    <a-modal v-model:visible="groupModalVisible" title="新增自选分组" @ok="handleCreateGroup" :confirmLoading="groupLoading">
      <a-form :model="groupForm" layout="vertical">
        <a-form-item label="分组名称" required>
          <a-input v-model:value="groupForm.name" placeholder="请输入分组名称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { 
  getWatchlistGroups, 
  getWatchlistStocks, 
  createWatchlistGroup, 
  deleteWatchlistGroup, 
  addStockToWatchlist, 
  removeStockFromWatchlist,
  reorderWatchlistStocks,
  type WatchlistGroupVO,
  type WatchlistStockVO
} from '@/api/watchlist';

const groups = ref<WatchlistGroupVO[]>([]);
const activeGroupId = ref<number>();
const stocks = ref<WatchlistStockVO[]>([]);
const loading = ref(false);
const addLoading = ref(false);
const newStockCode = ref('');

// Group Modal
const groupModalVisible = ref(false);
const groupLoading = ref(false);
const groupForm = reactive({ name: '' });

const columns = [
  { title: '股票代码', dataIndex: 'stockCode', key: 'stockCode' },
  { title: '股票名称', dataIndex: 'stockName', key: 'stockName' },
  { title: '最新价', dataIndex: 'latestPrice', key: 'latestPrice' },
  { title: '涨跌幅', dataIndex: 'changePercent', key: 'changePercent' },
  { title: '操作', key: 'operation', width: 100 },
];

const fetchGroups = async () => {
  try {
    const res = await getWatchlistGroups();
    if (res.data.success) {
      groups.value = res.data.data;
      if (groups.value && groups.value.length > 0 && !activeGroupId.value) {
        activeGroupId.value = groups.value[0].id;
        fetchStocks(activeGroupId.value);
      }
    }
  } catch (error) {
    console.error(error);
  }
};

const fetchStocks = async (groupId: number) => {
  loading.value = true;
  try {
    const res = await getWatchlistStocks(groupId);
    if (res.data.success) {
      stocks.value = res.data.data;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleTabChange = (key: any) => {
  fetchStocks(key);
};

const onTabEdit = (targetKey: any, action: string) => {
  if (action === 'remove') {
    Modal.confirm({
      title: '确定删除该分组吗？',
      content: '删除分组将同步移除该分组下的所有自选股票。',
      onOk: async () => {
        try {
          const res = await deleteWatchlistGroup(targetKey);
          if (res.data.success) {
            message.success('删除成功');
            fetchGroups().then(() => {
              if (activeGroupId.value === targetKey) {
                activeGroupId.value = (groups.value && groups.value.length > 0) ? groups.value[0].id : undefined;
                if (activeGroupId.value) fetchStocks(activeGroupId.value);
                else stocks.value = [];
              }
            });
          }
        } catch (error) {
          console.error(error);
        }
      },
    });
  }
};

const showAddGroupModal = () => {
  groupForm.name = '';
  groupModalVisible.value = true;
};

const handleCreateGroup = async () => {
  if (!groupForm.name) {
    message.warning('请输入分组名称');
    return;
  }
  groupLoading.value = true;
  try {
    const res = await createWatchlistGroup(groupForm);
    if (res.data.success) {
      message.success('创建成功');
      groupModalVisible.value = false;
      fetchGroups();
    } else {
      message.error(res.data.message || '创建失败');
    }
  } catch (error) {
    console.error(error);
  } finally {
    groupLoading.value = false;
  }
};

const handleAddStock = async () => {
  if (!activeGroupId.value) {
    message.warning('请先创建分组');
    return;
  }
  if (!newStockCode.value || newStockCode.value.length < 6) {
    message.warning('请输入正确的股票代码');
    return;
  }
  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: activeGroupId.value,
      stockCode: newStockCode.value
    });
    if (res.data.success) {
      message.success('添加成功');
      newStockCode.value = '';
      fetchStocks(activeGroupId.value);
    }
  } catch (error) {
    console.error(error);
  } finally {
    addLoading.value = false;
  }
};

const handleRemoveStock = async (stockCode: string) => {
  if (!activeGroupId.value) return;
  try {
    const res = await removeStockFromWatchlist(activeGroupId.value, stockCode);
    if (res.data.success) {
      message.success('移除成功');
      fetchStocks(activeGroupId.value);
    }
  } catch (error) {
    console.error(error);
  }
};

// 拖拽排序逻辑
const draggedItem = ref<WatchlistStockVO | null>(null);

const customRow = (record: WatchlistStockVO) => {
  return {
    draggable: true,
    style: { cursor: 'move' },
    onDragstart: (event: DragEvent) => {
      draggedItem.value = record;
      if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'move';
      }
    },
    onDragover: (event: DragEvent) => {
      event.preventDefault();
    },
    onDrop: async (event: DragEvent) => {
      event.preventDefault();
      if (!draggedItem.value || draggedItem.value === record) return;

      const oldIndex = stocks.value.findIndex(s => s.stockCode === draggedItem.value?.stockCode);
      const newIndex = stocks.value.findIndex(s => s.stockCode === record.stockCode);

      if (oldIndex !== -1 && newIndex !== -1) {
        const newStocks = [...stocks.value];
        const [removed] = newStocks.splice(oldIndex, 1);
        if (removed) {
          newStocks.splice(newIndex, 0, removed);
          stocks.value = newStocks;

          // 同步到后端
          if (activeGroupId.value) {
            try {
              await reorderWatchlistStocks({
                groupId: activeGroupId.value,
                stockCodes: stocks.value.map(s => s.stockCode)
              });
              message.success('排序已更新');
            } catch (e) {
              console.error(e);
              message.error('排序更新失败');
            }
          }
        }
      }
    }
  };
};

onMounted(() => {
  fetchGroups();
});
</script>

<style scoped>
.watchlist-container {
  padding: 0;
}
</style>
