import api from '@/utils/request';

// ==================== 类型定义 ====================

export interface ResponseDTO<T> {
    success: boolean;
    code: number;
    message: string | null;
    data: T;
}

export interface PageResult<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}

// ==================== 请求VO ====================

export interface ArticleCreateReqVO {
    title: string;
    content: string;
    visibility?: number; // 0=私密, 1=公开
}

export interface ArticleUpdateReqVO {
    id: number;
    title: string;
    content: string;
    visibility: number; // 0=私密, 1=公开
}

export interface ArticleIdReqVO {
    id: number;
}

export interface ArticleVisibilityUpdateReqVO {
    id: number;
    visibility: number; // 0=私密, 1=公开
}

// ==================== 响应VO ====================

export interface ArticleCreateRespVO {
    id: number;
    title: string;
    visibility: number; // 0=私密, 1=公开
    createdAt: string;
}

export interface ArticleDetailVO {
    id: number;
    title: string;
    content: string;
    authorId: number;
    authorUsername: string;
    visibility: number; // 0=私密, 1=公开
    createdAt: string;
    updatedAt: string;
}

export interface ArticleListItemVO {
    id: number;
    title: string;
    summary: string;
    authorUsername: string;
    visibility: number; // 0=私密, 1=公开
    createdAt: string;
    updatedAt: string;
}

// ==================== API方法 ====================

/**
 * 创建文章
 */
export const createArticle = (data: ArticleCreateReqVO) => {
    return api.post<ResponseDTO<ArticleCreateRespVO>>('/article/create', data);
};

/**
 * 更新文章
 */
export const updateArticle = (data: ArticleUpdateReqVO) => {
    return api.post<ResponseDTO<void>>('/article/update', data);
};

/**
 * 删除文章
 */
export const deleteArticle = (data: ArticleIdReqVO) => {
    return api.post<ResponseDTO<void>>('/article/delete', data);
};

/**
 * 获取文章详情
 */
export const getArticleDetail = (id: number) => {
    return api.get<ResponseDTO<ArticleDetailVO>>('/article/detail', {
        params: { id }
    });
};

/**
 * 获取公开文章列表(支持搜索)
 * @param params.keyword - 可选，搜索关键词。不传则返回全部公开文章
 * @param params.page - 页码，默认1
 * @param params.size - 每页大小，默认20
 */
export const getPublicArticles = (params: { keyword?: string; page?: number; size?: number }) => {
    return api.get<ResponseDTO<PageResult<ArticleListItemVO>>>('/article/public/list', {
        params: {
            keyword: params.keyword,
            page: params.page || 1,
            size: params.size || 20
        }
    });
};

/**
 * 获取个人文章列表(支持搜索)
 * @param params.keyword - 可选，搜索关键词。不传则返回全部个人文章
 * @param params.page - 页码，默认1
 * @param params.size - 每页大小，默认20
 */
export const getUserArticles = (params: { keyword?: string; page?: number; size?: number }) => {
    return api.get<ResponseDTO<PageResult<ArticleListItemVO>>>('/article/my/list', {
        params: {
            keyword: params.keyword,
            page: params.page || 1,
            size: params.size || 20
        }
    });
};

/**
 * 更新文章可见性
 */
export const updateArticleVisibility = (data: ArticleVisibilityUpdateReqVO) => {
    return api.post<ResponseDTO<void>>('/article/update-visibility', data);
};
