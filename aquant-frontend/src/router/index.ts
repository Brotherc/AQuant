
import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layout/BasicLayout.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            component: BasicLayout,
            redirect: '/stock-data/index',
            children: [
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
                    path: 'history',
                    name: 'HistoryRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'HistoryQuotes',
                            component: () => import('@/views/history/HistoryQuotes.vue')
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
                    path: 'strategy',
                    name: 'StrategyRoot',
                    children: [
                        {
                            path: 'dual-ma',
                            name: 'DualMA',
                            component: () => import('@/views/strategy/DualMA.vue')
                        }
                    ]
                }
            ]
        }
    ]
})

export default router
