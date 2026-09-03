
import { createRouter, createWebHistory } from 'vue-router'
import { Modal } from 'ant-design-vue';
import BasicLayout from '@/layout/BasicLayout.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'Login',
            component: () => import('@/views/login/Login.vue')
        },
        {
            path: '/',
            component: BasicLayout,
            redirect: '/dashboard',
            children: [
                {
                    path: 'dashboard',
                    name: 'DashboardRoot',
                    children: [
                        {
                            path: '',
                            name: 'Dashboard',
                            component: () => import('@/views/dashboard/Dashboard.vue')
                        }
                    ]
                },
                {
                    path: 'watchlist',
                    name: 'WatchlistRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'Watchlist',
                            component: () => import('@/views/watchlist/Watchlist.vue'),
                            meta: { requiresAuth: true }
                        }
                    ]
                },
                {
                    path: 'stock-data',
                    name: 'StockDataRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'StockData',
                            component: () => import('@/views/stock-data/StockData.vue')
                        }
                    ]
                },
                {
                    path: 'board',
                    name: 'BoardRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'BoardData',
                            component: () => import('@/views/board/BoardData.vue')
                        }
                    ]
                },
                {
                    path: 'industry-analysis',
                    name: 'IndustryAnalysisRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'IndustryRiseAnalysis',
                            component: () => import('@/views/industry-analysis/IndustryRiseAnalysis.vue')
                        }
                    ]
                },
                {
                    path: 'industry-detail',
                    name: 'IndustryDetailRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'IndustryDetail',
                            component: () => import('@/views/industry-detail/IndustryDetail.vue')
                        }
                    ]
                },
                {
                    path: 'fund',
                    name: 'FundRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'Fund',
                            component: () => import('@/views/fund/Fund.vue')
                        }
                    ]
                },

                {
                    path: 'indicators',
                    name: 'IndicatorsRoot',
                    children: [
                        {
                            path: 'dupont',
                            name: 'DupontAnalysis',
                            component: () => import('@/views/indicators/DupontAnalysis.vue')
                        },
                        {
                            path: 'growth',
                            name: 'GrowthMetrics',
                            component: () => import('@/views/indicators/GrowthMetrics.vue')
                        },
                        {
                            path: 'valuation',
                            name: 'ValuationMetrics',
                            component: () => import('@/views/indicators/ValuationMetrics.vue')
                        }
                    ]
                },
                {
                    path: 'dividend',
                    name: 'DividendRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'StockDividend',
                            component: () => import('@/views/dividend/StockDividend.vue')
                        }
                    ]
                },
                {
                    path: 'strategy',
                    name: 'StrategyRoot',
                    redirect: '/strategy/index',
                    children: [
                        {
                            path: 'index',
                            name: 'StrategyIndex',
                            component: () => import('@/views/strategy/StrategyIndex.vue')
                        },
                        {
                            path: 'dual-ma',
                            name: 'DualMA',
                            redirect: '/strategy/index?type=dual-ma'
                        },
                        {
                            path: 'momentum',
                            name: 'Momentum',
                            redirect: '/strategy/index?type=momentum'
                        },
                        {
                            path: 'macd',
                            name: 'MACD',
                            redirect: '/strategy/index?type=macd'
                        }
                    ]
                },
                {
                    path: 'finance-sites',
                    name: 'FinanceSitesRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'FinanceSites',
                            component: () => import('@/views/finance-sites/FinanceSites.vue')
                        }
                    ]
                },
                {
                    path: 'article',
                    name: 'ArticleRoot',
                    children: [
                        {
                            path: 'public',
                            name: 'PublicArticles',
                            component: () => import('@/views/article/ArticleList.vue'),
                            props: { isMyArticles: false }
                        },
                        {
                            path: 'my',
                            name: 'MyArticles',
                            component: () => import('@/views/article/ArticleList.vue'),
                            props: { isMyArticles: true },
                            meta: { requiresAuth: true }
                        },
                        {
                            path: 'detail/:id',
                            name: 'ArticleDetail',
                            component: () => import('@/views/article/ArticleDetail.vue')
                        },
                        {
                            path: 'create',
                            name: 'ArticleCreate',
                            component: () => import('@/views/article/ArticleEdit.vue'),
                            meta: { requiresAuth: true }
                        },
                        {
                            path: 'edit/:id',
                            name: 'ArticleEdit',
                            component: () => import('@/views/article/ArticleEdit.vue'),
                            meta: { requiresAuth: true }
                        }
                    ]
                }
            ]
        }
    ]
})

let authPromptVisible = false;

router.beforeEach((to, from) => {
    if (to.meta.requiresAuth && !localStorage.getItem('token')) {
        return new Promise((resolve) => {
            if (authPromptVisible) {
                resolve(false);
                return;
            }

            authPromptVisible = true;
            const hasPreviousRoute = from.matched.length > 0;

            Modal.confirm({
                title: '请先登录',
                content: '当前页面需要登录后才能访问。',
                okText: '去登录',
                cancelText: hasPreviousRoute ? '取消' : '返回首页',
                centered: true,
                onOk: () => {
                    resolve({
                        path: '/login',
                        query: { redirect: to.fullPath }
                    });
                },
                onCancel: () => {
                    resolve(hasPreviousRoute ? false : { path: '/stock-data/index' });
                },
                afterClose: () => {
                    authPromptVisible = false;
                }
            });
        });
    }
});

export default router
