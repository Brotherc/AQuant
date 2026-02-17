
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
                    path: 'watchlist',
                    name: 'WatchlistRoot',
                    children: [
                        {
                            path: 'index',
                            name: 'Watchlist',
                            component: () => import('@/views/watchlist/Watchlist.vue')
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
