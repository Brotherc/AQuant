<template>
  <div class="tiptap-editor">
    <!-- 工具栏 -->
    <div v-if="editor" class="editor-toolbar">
      <div class="toolbar-group">
        <button
          @click="editor.chain().focus().toggleBold().run()"
          :class="{ 'is-active': editor.isActive('bold') }"
          class="toolbar-button"
          type="button"
        >
          <BoldOutlined />
        </button>
        <button
          @click="editor.chain().focus().toggleItalic().run()"
          :class="{ 'is-active': editor.isActive('italic') }"
          class="toolbar-button"
          type="button"
        >
          <ItalicOutlined />
        </button>
        <button
          @click="editor.chain().focus().toggleStrike().run()"
          :class="{ 'is-active': editor.isActive('strike') }"
          class="toolbar-button"
          type="button"
        >
          <StrikethroughOutlined />
        </button>
        <button
          @click="editor.chain().focus().toggleCode().run()"
          :class="{ 'is-active': editor.isActive('code') }"
          class="toolbar-button"
          type="button"
        >
          <CodeOutlined />
        </button>
      </div>

      <div class="toolbar-divider"></div>

      <div class="toolbar-group">
        <button
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
          :class="{ 'is-active': editor.isActive('heading', { level: 1 }) }"
          class="toolbar-button"
          type="button"
        >
          H1
        </button>
        <button
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
          :class="{ 'is-active': editor.isActive('heading', { level: 2 }) }"
          class="toolbar-button"
          type="button"
        >
          H2
        </button>
        <button
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
          :class="{ 'is-active': editor.isActive('heading', { level: 3 }) }"
          class="toolbar-button"
          type="button"
        >
          H3
        </button>
      </div>

      <div class="toolbar-divider"></div>

      <div class="toolbar-group">
        <button
          @click="editor.chain().focus().toggleBulletList().run()"
          :class="{ 'is-active': editor.isActive('bulletList') }"
          class="toolbar-button"
          type="button"
        >
          <UnorderedListOutlined />
        </button>
        <button
          @click="editor.chain().focus().toggleOrderedList().run()"
          :class="{ 'is-active': editor.isActive('orderedList') }"
          class="toolbar-button"
          type="button"
        >
          <OrderedListOutlined />
        </button>
        <button
          @click="editor.chain().focus().toggleCodeBlock().run()"
          :class="{ 'is-active': editor.isActive('codeBlock') }"
          class="toolbar-button"
          type="button"
        >
          <FileTextOutlined />
        </button>
        <button
          @click="editor.chain().focus().toggleBlockquote().run()"
          :class="{ 'is-active': editor.isActive('blockquote') }"
          class="toolbar-button"
          type="button"
        >
          <MessageOutlined />
        </button>
      </div>

      <div class="toolbar-divider"></div>

      <div class="toolbar-group">
        <button
          @click="setLink"
          :class="{ 'is-active': editor.isActive('link') }"
          class="toolbar-button"
          type="button"
        >
          <LinkOutlined />
        </button>
        <button
          @click="editor.chain().focus().setHorizontalRule().run()"
          class="toolbar-button"
          type="button"
        >
          <MinusOutlined />
        </button>
      </div>

      <div class="toolbar-divider"></div>

      <div class="toolbar-group">
        <button
          @click="editor.chain().focus().undo().run()"
          :disabled="!editor.can().undo()"
          class="toolbar-button"
          type="button"
        >
          <UndoOutlined />
        </button>
        <button
          @click="editor.chain().focus().redo().run()"
          :disabled="!editor.can().redo()"
          class="toolbar-button"
          type="button"
        >
          <RedoOutlined />
        </button>
      </div>
    </div>

    <!-- 编辑器内容区 -->
    <editor-content :editor="editor" class="editor-content" />

    <!-- 字符计数 -->
    <div class="editor-footer">
      <span class="char-count">{{ characterCount }} / {{ maxLength }} 字符</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue';
import { useEditor, EditorContent } from '@tiptap/vue-3';
import StarterKit from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';
import Link from '@tiptap/extension-link';
import {
  BoldOutlined,
  ItalicOutlined,
  StrikethroughOutlined,
  CodeOutlined,
  UnorderedListOutlined,
  OrderedListOutlined,
  FileTextOutlined,
  MessageOutlined,
  LinkOutlined,
  MinusOutlined,
  UndoOutlined,
  RedoOutlined
} from '@ant-design/icons-vue';

const props = defineProps<{
  modelValue: string;
  placeholder?: string;
  maxLength?: number;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const maxLength = computed(() => props.maxLength || 50000);

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [1, 2, 3]
      }
    }),
    Placeholder.configure({
      placeholder: props.placeholder || '开始写作...'
    }),
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        target: '_blank',
        rel: 'noopener noreferrer'
      }
    })
  ],
  editorProps: {
    attributes: {
      class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl focus:outline-none'
    }
  },
  onUpdate: ({ editor }) => {
    const html = editor.getHTML();
    emit('update:modelValue', html);
  }
});

const characterCount = computed(() => {
  if (!editor.value) return 0;
  return editor.value.state.doc.textContent.length;
});

// 设置链接
const setLink = () => {
  if (!editor.value) return;

  const previousUrl = editor.value.getAttributes('link').href;
  const url = window.prompt('请输入链接地址:', previousUrl);

  if (url === null) {
    return;
  }

  if (url === '') {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run();
    return;
  }

  editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run();
};

// 监听外部值变化
watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value) return;
    const isSame = editor.value.getHTML() === value;
    if (isSame) return;
    editor.value.commands.setContent(value, false);
  }
);

onBeforeUnmount(() => {
  editor.value?.destroy();
});
</script>

<style scoped>
.tiptap-editor {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  transition: border-color 0.3s;
}

.tiptap-editor:focus-within {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.toolbar-group {
  display: flex;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  background: #d9d9d9;
  margin: 0 4px;
}

.toolbar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  color: #595959;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s;
}

.toolbar-button:hover:not(:disabled) {
  background: #e6f7ff;
  color: #1890ff;
}

.toolbar-button.is-active {
  background: #1890ff;
  color: #fff;
}

.toolbar-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.editor-content {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  padding: 16px;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  padding: 8px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.char-count {
  font-size: 12px;
  color: #8c8c8c;
}

/* Tiptap 编辑器样式 */
:deep(.ProseMirror) {
  outline: none;
  min-height: 400px;
}

:deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: #adb5bd;
  pointer-events: none;
  height: 0;
}

:deep(.ProseMirror h1) {
  font-size: 2em;
  font-weight: 700;
  margin: 0.67em 0;
  line-height: 1.3;
}

:deep(.ProseMirror h2) {
  font-size: 1.5em;
  font-weight: 600;
  margin: 0.75em 0;
  line-height: 1.4;
}

:deep(.ProseMirror h3) {
  font-size: 1.17em;
  font-weight: 600;
  margin: 0.83em 0;
  line-height: 1.5;
}

:deep(.ProseMirror p) {
  margin: 1em 0;
  line-height: 1.6;
}

:deep(.ProseMirror ul),
:deep(.ProseMirror ol) {
  padding-left: 2em;
  margin: 1em 0;
}

:deep(.ProseMirror li) {
  margin: 0.5em 0;
}

:deep(.ProseMirror code) {
  background-color: #f5f5f5;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

:deep(.ProseMirror pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
}

:deep(.ProseMirror pre code) {
  background: none;
  padding: 0;
  color: inherit;
  font-size: 0.9em;
}

:deep(.ProseMirror blockquote) {
  border-left: 4px solid #1890ff;
  padding-left: 1em;
  margin: 1em 0;
  color: #595959;
  font-style: italic;
}

:deep(.ProseMirror a) {
  color: #1890ff;
  text-decoration: underline;
  cursor: pointer;
}

:deep(.ProseMirror a:hover) {
  color: #40a9ff;
}

:deep(.ProseMirror hr) {
  border: none;
  border-top: 2px solid #f0f0f0;
  margin: 2em 0;
}

:deep(.ProseMirror strong) {
  font-weight: 700;
}

:deep(.ProseMirror em) {
  font-style: italic;
}

:deep(.ProseMirror s) {
  text-decoration: line-through;
}
</style>
