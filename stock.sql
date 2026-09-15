DROP TABLE IF EXISTS `stock_abnormal`;
CREATE TABLE `stock_abnormal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `abnormal_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '异常原因',
  `abnormal_source` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '异常来源',
  `abnormal_type` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '异常类型，例如 DELISTED',
  `code` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票代码，例如 sh600001',
  `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票名称',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `update_time` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='异常股票归档表';

DROP TABLE IF EXISTS `stock_balance_sheet`;
CREATE TABLE `stock_balance_sheet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `accounts_payable` decimal(38,2) DEFAULT NULL COMMENT '应付账款（元）',
  `accounts_receivable` decimal(38,2) DEFAULT NULL COMMENT '应收账款（元）',
  `advance_receipts` decimal(38,2) DEFAULT NULL COMMENT '预收款项 / 合同负债（元）',
  `announcement_date` date DEFAULT NULL COMMENT '最新公告披露日期',
  `asset_liability_ratio` decimal(38,2) DEFAULT NULL COMMENT '资产负债率（%）',
  `create_time` datetime(6) DEFAULT NULL COMMENT '记录创建时间',
  `inventory` decimal(38,2) DEFAULT NULL COMMENT '存货（元）',
  `monetary_funds` decimal(38,2) DEFAULT NULL COMMENT '货币资金（元）',
  `report_date` date DEFAULT NULL COMMENT '报告期（如 2025-12-31、2025-09-30 等）',
  `stock_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '股票代码（纯数字，不带交易所前缀，如 600519）',
  `stock_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票简称（如 贵州茅台）',
  `total_assets` decimal(38,2) DEFAULT NULL COMMENT '资产总计 / 总资产（元）',
  `total_assets_yoy` decimal(38,2) DEFAULT NULL COMMENT '资产总计同比增长率（%）',
  `total_equity` decimal(38,2) DEFAULT NULL COMMENT '所有者权益合计 / 归属于母公司股东权益合计（净资产，元）',
  `total_liabilities` decimal(38,2) DEFAULT NULL COMMENT '负债合计 / 总负债（元）',
  `total_liabilities_yoy` decimal(38,2) DEFAULT NULL COMMENT '负债合计同比增长率（%）',
  `update_time` datetime(6) DEFAULT NULL COMMENT '记录更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='股票资产负债表';

DROP TABLE IF EXISTS `stock_board_constituent`;
CREATE TABLE `stock_board_constituent` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `board_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `source_updated_at` datetime(6) NOT NULL,
  `stock_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stock_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_constituent_stock` (`board_code`,`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行业当前成分股';

DROP TABLE IF EXISTS `stock_board_constituent_em`;
CREATE TABLE `stock_board_constituent_em` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `board_code` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '东方财富行业板块代码',
  `source_updated_at` datetime(6) NOT NULL COMMENT '数据源更新时间',
  `stock_code` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '成分股票代码',
  `stock_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '成分股票名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_constituent_em_stock` (`board_code`,`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='东方财富行业当前成分股';

DROP TABLE IF EXISTS `stock_board_constituent_quote`;
CREATE TABLE `stock_board_constituent_quote` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `board_code` varchar(16) NOT NULL COMMENT '板块代码',
  `stock_code` varchar(16) NOT NULL COMMENT '股票代码',
  `stock_name` varchar(50) NOT NULL COMMENT '股票名称',
  `latest_price` decimal(10,2) DEFAULT NULL COMMENT '最新价',
  `change_percent` decimal(8,3) DEFAULT NULL COMMENT '涨跌幅(%)',
  `change_amount` decimal(10,2) DEFAULT NULL COMMENT '涨跌额',
  `volume` bigint DEFAULT NULL COMMENT '成交量(股)',
  `turnover` decimal(20,2) DEFAULT NULL COMMENT '成交额(元)',
  `amplitude` decimal(8,3) DEFAULT NULL COMMENT '振幅(%)',
  `high_price` decimal(10,2) DEFAULT NULL COMMENT '最高价',
  `low_price` decimal(10,2) DEFAULT NULL COMMENT '最低价',
  `open_price` decimal(10,2) DEFAULT NULL COMMENT '今开',
  `prev_close` decimal(10,2) DEFAULT NULL COMMENT '昨收',
  `turnover_rate` decimal(8,3) DEFAULT NULL COMMENT '换手率(%)',
  `pe_ttm` decimal(10,2) DEFAULT NULL COMMENT '市盈率-动态',
  `pb` decimal(10,2) DEFAULT NULL COMMENT '市净率',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '行情采集时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_board_constituent_stock` (`board_code`,`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='板块成份股最新行情表';

DROP TABLE IF EXISTS `stock_dividend`;
CREATE TABLE `stock_dividend` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stock_code` varchar(10) NOT NULL COMMENT '股票代码',
  `stock_name` varchar(50) NOT NULL COMMENT '股票名称',
  `bonus_share_total_ratio` decimal(10,4) DEFAULT NULL COMMENT '送转股份-送转总比例',
  `bonus_share_ratio` decimal(10,4) DEFAULT NULL COMMENT '送转股份-送股比例',
  `transfer_share_ratio` decimal(10,4) DEFAULT NULL COMMENT '送转股份-转股比例',
  `cash_dividend_ratio` decimal(10,4) DEFAULT NULL COMMENT '现金分红比例(每10股/每股)',
  `dividend_yield` decimal(20,10) DEFAULT NULL COMMENT '股息率(%)',
  `earnings_per_share` decimal(10,4) DEFAULT NULL COMMENT '每股收益',
  `net_asset_per_share` decimal(20,10) DEFAULT NULL COMMENT '每股净资产',
  `capital_reserve_per_share` decimal(20,10) DEFAULT NULL COMMENT '每股公积金',
  `undistributed_profit_per_share` decimal(20,10) DEFAULT NULL COMMENT '每股未分配利润',
  `net_profit_growth_rate` decimal(20,10) DEFAULT NULL COMMENT '净利润同比增长率(%)',
  `total_shares` bigint DEFAULT NULL COMMENT '总股本',
  `proposal_announcement_date` date DEFAULT NULL COMMENT '预案公告日',
  `record_date` date DEFAULT NULL COMMENT '股权登记日',
  `ex_dividend_date` date DEFAULT NULL COMMENT '除权除息日',
  `latest_announcement_date` date DEFAULT NULL COMMENT '最新公告日期',
  `plan_status` varchar(30) DEFAULT NULL COMMENT '方案进度(预案/通过/实施/终止等)',
  `report_date` varchar(8) DEFAULT NULL COMMENT '报告日期',
  PRIMARY KEY (`id`),
  KEY `idx_latest_announcement_date` (`latest_announcement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票分红送转方案表';

DROP TABLE IF EXISTS `stock_dupont_analysis`;
CREATE TABLE `stock_dupont_analysis` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stock_code` varchar(16) NOT NULL COMMENT '股票代码',
  `stock_name` varchar(64) NOT NULL COMMENT '股票简称',
  `roe_3y_avg` decimal(20,10) DEFAULT NULL COMMENT 'ROE-3年平均',
  `roe_3y_avg_industry_med` decimal(20,10) DEFAULT NULL COMMENT 'ROE-3年平均-行业中值',
  `roe_3y_avg_industry_avg` decimal(20,10) DEFAULT NULL COMMENT 'ROE-3年平均-行业平均',
  `roe_last_3y_a` decimal(20,10) DEFAULT NULL COMMENT 'ROE-3年前实际',
  `roe_last_3y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT 'ROE-3年前实际-行业中值',
  `roe_last_3y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT 'ROE-3年前实际-行业平均',
  `roe_last_2y_a` decimal(20,10) DEFAULT NULL COMMENT 'ROE-2前年实际',
  `roe_last_2y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT 'ROE-2前年实际-行业中值',
  `roe_last_2y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT 'ROE-2前年实际-行业平均',
  `roe_last_y_a` decimal(20,10) DEFAULT NULL COMMENT 'ROE-去年实际',
  `roe_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT 'ROE-去年实际-行业中值',
  `roe_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT 'ROE-去年实际-行业平均',
  `net_margin_3y_avg` decimal(20,10) DEFAULT NULL COMMENT '净利率-3年平均',
  `net_margin_3y_avg_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利率-3年平均-行业中值',
  `net_margin_3y_avg_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利率-3年平均-行业平均',
  `net_margin_last_3y_a` decimal(20,10) DEFAULT NULL COMMENT '净利率-3年前实际',
  `net_margin_last_3y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利率-3年前实际-行业中值',
  `net_margin_last_3y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利率-3年前实际-行业平均',
  `net_margin_last_2y_a` decimal(20,10) DEFAULT NULL COMMENT '净利率-2前年实际',
  `net_margin_last_2y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利率-2前年实际-行业中值',
  `net_margin_last_2y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利率-2前年实际-行业平均',
  `net_margin_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '净利率-去年实际',
  `net_margin_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利率-去年实际-行业中值',
  `net_margin_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利率-去年实际-行业平均',
  `asset_turnover_3y_avg` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-3年平均',
  `asset_turnover_3y_avg_industry_med` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-3年平均-行业中值',
  `asset_turnover_3y_avg_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-3年平均-行业平均',
  `asset_turnover_last_3y_a` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-3年前实际',
  `asset_turnover_last_3y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-3年前实际-行业中值',
  `asset_turnover_last_3y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-3年前实际-行业平均',
  `asset_turnover_last_2y_a` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-2前年实际',
  `asset_turnover_last_2y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-2前年实际-行业中值',
  `asset_turnover_last_2y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-2前年实际-行业平均',
  `asset_turnover_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-去年实际',
  `asset_turnover_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-去年实际-行业中值',
  `asset_turnover_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '总资产周转率-去年实际-行业平均',
  `equity_multiplier_3y_avg` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-3年平均',
  `equity_multiplier_3y_avg_industry_med` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-3年平均-行业中值',
  `equity_multiplier_3y_avg_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-3年平均-行业平均',
  `equity_multiplier_last_3y_a` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-3年前实际',
  `equity_multiplier_last_3y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-3年前实际-行业中值',
  `equity_multiplier_last_3y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-3年前实际-行业平均',
  `equity_multiplier_last_2y_a` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-2前年实际',
  `equity_multiplier_last_2y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-2前年实际-行业中值',
  `equity_multiplier_last_2y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-2前年实际-行业平均',
  `equity_multiplier_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-去年实际',
  `equity_multiplier_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-去年实际-行业中值',
  `equity_multiplier_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '权益乘数-去年实际-行业平均',
  `roe_3y_avg_rank` decimal(20,10) DEFAULT NULL COMMENT 'ROE-3年平均排名',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `conclusion` varchar(255) DEFAULT NULL COMMENT '结论摘要',
  `industry` varchar(255) DEFAULT NULL COMMENT '所处行业',
  `quality_level` varchar(255) DEFAULT NULL COMMENT '质量等级 (优秀 / 良好 / 中等 / 较差)',
  `quality_score` decimal(38,2) DEFAULT NULL COMMENT '杜邦质量评分 (0-100)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_code` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票杜邦分析表';

DROP TABLE IF EXISTS `stock_fund_announcement_sync`;
CREATE TABLE `stock_fund_announcement_sync` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `announcement_id` varchar(100) NOT NULL COMMENT '来源站点公告ID',
  `source` varchar(32) NOT NULL COMMENT '基金管理人来源编码',
  `title` varchar(500) DEFAULT NULL COMMENT '公告标题',
  `announcement_date` date DEFAULT NULL COMMENT '公告日期',
  `detail_url` varchar(1000) DEFAULT NULL COMMENT '公告详情地址',
  `attachment_url` varchar(1000) DEFAULT NULL COMMENT '附件地址',
  `attachment_hash` varchar(64) DEFAULT NULL COMMENT '附件SHA-256',
  `status` varchar(16) NOT NULL COMMENT 'SUCCESS/IGNORED/FAILED/PENDING',
  `failure_count` int NOT NULL DEFAULT '0' COMMENT '失败次数',
  `last_error` varchar(1000) DEFAULT NULL COMMENT '最近错误',
  `processed_time` datetime DEFAULT NULL COMMENT '处理时间',
  `retry_after_date` date DEFAULT NULL COMMENT '未来规则最早重试日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fund_announcement_source_id` (`source`,`announcement_id`),
  KEY `idx_fund_announcement_status` (`source`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基金管理人公告处理记录';

DROP TABLE IF EXISTS `stock_fund_info`;
CREATE TABLE `stock_fund_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '基金代码',
  `pinyin_abbr` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拼音缩写',
  `fund_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '基金简称',
  `fund_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '基金类型',
  `pinyin_full` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拼音全称',
  `purchase_start_amount` decimal(20,4) DEFAULT NULL COMMENT '购买起点',
  `daily_limit_amount` decimal(24,4) DEFAULT NULL COMMENT '日累计限定金额',
  `fee_rate` decimal(10,4) DEFAULT NULL COMMENT '手续费',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `latest_net_value_report_date` date DEFAULT NULL COMMENT '最新净值报告日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fund_code` (`fund_code`),
  KEY `idx_fund_name` (`fund_name`),
  KEY `idx_fund_type` (`fund_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金基础信息表';

DROP TABLE IF EXISTS `stock_fund_net_value`;
CREATE TABLE `stock_fund_net_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '基金代码',
  `nav_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '净值日期',
  `unit_nav` decimal(16,6) DEFAULT NULL COMMENT '单位净值',
  `daily_growth_rate` decimal(10,4) DEFAULT NULL COMMENT '日增长率',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fund_code_nav_date` (`fund_code`,`nav_date`),
  KEY `idx_fund_code` (`fund_code`),
  KEY `idx_nav_date` (`nav_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金净值表';

DROP TABLE IF EXISTS `stock_fund_portfolio_holding`;
CREATE TABLE `stock_fund_portfolio_holding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(32) NOT NULL COMMENT '基金代码',
  `report_year` int NOT NULL COMMENT '报告年份',
  `report_quarter` int NOT NULL COMMENT '报告季度',
  `seq_no` int DEFAULT NULL COMMENT '序号',
  `stock_code` varchar(32) DEFAULT NULL COMMENT '股票代码',
  `stock_name` varchar(128) DEFAULT NULL COMMENT '股票名称',
  `net_value_ratio` decimal(10,4) DEFAULT NULL COMMENT '占净值比例',
  `hold_shares` decimal(20,4) DEFAULT NULL COMMENT '持股数',
  `market_value` decimal(20,4) DEFAULT NULL COMMENT '持仓市值',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基金持仓明细表';

DROP TABLE IF EXISTS `stock_fund_purchase_limit`;
CREATE TABLE `stock_fund_purchase_limit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fund_code` varchar(20) NOT NULL COMMENT '基金代码',
  `source` varchar(32) NOT NULL COMMENT '基金管理人来源编码',
  `source_name` varchar(100) NOT NULL COMMENT '基金管理人名称',
  `sales_channel` varchar(32) NOT NULL COMMENT '销售渠道编码',
  `sales_channel_name` varchar(100) NOT NULL COMMENT '销售渠道名称',
  `business_type` varchar(32) NOT NULL COMMENT '业务类型',
  `status` varchar(32) NOT NULL COMMENT 'OPEN/LIMITED/SUSPENDED',
  `limit_amount` decimal(20,4) DEFAULT NULL COMMENT '限额',
  `currency` varchar(16) DEFAULT NULL COMMENT '币种',
  `effective_date` date DEFAULT NULL COMMENT '生效日期',
  `announcement_date` date DEFAULT NULL COMMENT '公告日期',
  `announcement_id` varchar(100) DEFAULT NULL COMMENT '来源站点公告ID',
  `announcement_title` varchar(500) DEFAULT NULL COMMENT '公告标题',
  `announcement_url` varchar(1000) DEFAULT NULL COMMENT '公告地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fund_limit_current` (`source`,`fund_code`,`sales_channel`,`business_type`),
  KEY `idx_fund_limit_code` (`fund_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='基金管理人官方渠道当前申购限制';

DROP TABLE IF EXISTS `stock_growth_metrics`;
CREATE TABLE `stock_growth_metrics` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stock_code` varchar(16) NOT NULL COMMENT '股票代码',
  `stock_name` varchar(64) NOT NULL COMMENT '股票简称',
  `eps_growth_3y_cagr` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-3年复合',
  `eps_growth_3y_cagr_industry_med` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-3年复合-行业中值',
  `eps_growth_3y_cagr_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-3年复合-行业平均',
  `eps_growth_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-去年实际',
  `eps_growth_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-去年实际-行业中值',
  `eps_growth_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-去年实际-行业平均',
  `eps_growth_ttm` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-TTM',
  `eps_growth_ttm_industry_med` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-TTM-行业中值',
  `eps_growth_ttm_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-TTM-行业平均',
  `eps_growth_this_y_e` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-今年预测',
  `eps_growth_this_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-今年预测-行业中值',
  `eps_growth_this_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-今年预测-行业平均',
  `eps_growth_next_y_e` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-明年预测',
  `eps_growth_next_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-明年预测-行业中值',
  `eps_growth_next_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-明年预测-行业平均',
  `eps_growth_next_2y_e` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-后年预测',
  `eps_growth_next_2y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-后年预测-行业中值',
  `eps_growth_next_2y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-后年预测-行业平均',
  `eps_growth_3y_cagr_rank` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-3年复合排名',
  `eps_growth_3y_cagr_rank_industry_med` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-3年复合排名-行业中值',
  `eps_growth_3y_cagr_rank_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '基本每股收益增长率-3年复合排名-行业平均',
  `revenue_growth_3y_cagr` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-3年复合',
  `revenue_growth_3y_cagr_industry_med` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-3年复合-行业中值',
  `revenue_growth_3y_cagr_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-3年复合-行业平均',
  `revenue_growth_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-去年实际',
  `revenue_growth_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-去年实际-行业中值',
  `revenue_growth_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-去年实际-行业平均',
  `revenue_growth_ttm` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-TTM',
  `revenue_growth_ttm_industry_med` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-TTM-行业中值',
  `revenue_growth_ttm_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-TTM-行业平均',
  `revenue_growth_this_y_e` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-今年预测',
  `revenue_growth_this_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-今年预测-行业中值',
  `revenue_growth_this_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-今年预测-行业平均',
  `revenue_growth_next_y_e` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-明年预测',
  `revenue_growth_next_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-明年预测-行业中值',
  `revenue_growth_next_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-明年预测-行业平均',
  `revenue_growth_next_2y_e` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-后年预测',
  `revenue_growth_next_2y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-后年预测-行业中值',
  `revenue_growth_next_2y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '营业收入增长率-后年预测-行业平均',
  `net_profit_growth_3y_cagr` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-3年复合',
  `net_profit_growth_3y_cagr_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-3年复合-行业中值',
  `net_profit_growth_3y_cagr_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-3年复合-行业平均',
  `net_profit_growth_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-去年实际',
  `net_profit_growth_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-去年实际-行业中值',
  `net_profit_growth_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-去年实际-行业平均',
  `net_profit_growth_ttm` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-TTM',
  `net_profit_growth_ttm_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-TTM-行业中值',
  `net_profit_growth_ttm_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-TTM-行业平均',
  `net_profit_growth_this_y_e` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-今年预测',
  `net_profit_growth_this_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-今年预测-行业中值',
  `net_profit_growth_this_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-今年预测-行业平均',
  `net_profit_growth_next_y_e` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-明年预测',
  `net_profit_growth_next_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-明年预测-行业中值',
  `net_profit_growth_next_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-明年预测-行业平均',
  `net_profit_growth_next_2y_e` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-后年预测',
  `net_profit_growth_next_2y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-后年预测-行业中值',
  `net_profit_growth_next_2y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '净利润增长率-后年预测-行业平均',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `conclusion` varchar(500) DEFAULT NULL COMMENT '成长结论 / 简要解读',
  `eps_growth_last_2y_a` decimal(38,2) DEFAULT NULL COMMENT '基本每股收益增长率-2年前实际',
  `eps_growth_last_3y_a` decimal(38,2) DEFAULT NULL COMMENT '基本每股收益增长率-3年前实际',
  `growth_level` varchar(255) DEFAULT NULL COMMENT '成长等级 (优秀, 良好, 中等, 较弱)',
  `growth_score` decimal(38,2) DEFAULT NULL COMMENT '成长评分 (0~100)',
  `industry` varchar(255) DEFAULT NULL COMMENT '所属行业',
  `net_profit_growth_last_2y_a` decimal(38,2) DEFAULT NULL COMMENT '净利润增长率-2年前实际',
  `net_profit_growth_last_3y_a` decimal(38,2) DEFAULT NULL COMMENT '净利润增长率-3年前实际',
  `revenue_growth_last_2y_a` decimal(38,2) DEFAULT NULL COMMENT '营业收入增长率-2年前实际',
  `revenue_growth_last_3y_a` decimal(38,2) DEFAULT NULL COMMENT '营业收入增长率-3年前实际',
  PRIMARY KEY (`id`),
  KEY `idx_stock_code` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票/行业成长性指标表';

DROP TABLE IF EXISTS `stock_index_history`;
CREATE TABLE `stock_index_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `close_price` decimal(38,2) DEFAULT NULL COMMENT '收盘价',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `high_price` decimal(38,2) DEFAULT NULL COMMENT '最高价',
  `index_code` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指数代码，如 sh000001',
  `index_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指数名称，如 上证指数',
  `low_price` decimal(38,2) DEFAULT NULL COMMENT '最低价',
  `open_price` decimal(38,2) DEFAULT NULL COMMENT '开盘价',
  `trade_date` date DEFAULT NULL COMMENT '交易日期',
  `turnover` decimal(38,2) DEFAULT NULL COMMENT '成交额',
  `volume` decimal(38,2) DEFAULT NULL COMMENT '成交量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='A股指数历史日 K 线表';

DROP TABLE IF EXISTS `stock_index_spot`;
CREATE TABLE `stock_index_spot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `change_amount` decimal(38,2) DEFAULT NULL COMMENT '涨跌额',
  `change_percent` decimal(38,2) DEFAULT NULL COMMENT '涨跌幅 (%)',
  `code` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指数代码，例如 sh000001',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `high_price` decimal(38,2) DEFAULT NULL COMMENT '最高价',
  `latest_price` decimal(38,2) DEFAULT NULL COMMENT '最新价',
  `low_price` decimal(38,2) DEFAULT NULL COMMENT '最低价',
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指数名称，例如 上证指数',
  `open_price` decimal(38,2) DEFAULT NULL COMMENT '今开',
  `prev_close` decimal(38,2) DEFAULT NULL COMMENT '昨收',
  `turnover` decimal(38,2) DEFAULT NULL COMMENT '成交额',
  `volume` decimal(38,2) DEFAULT NULL COMMENT '成交量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='A股股票指数实时行情表';

DROP TABLE IF EXISTS `stock_industry_board`;
CREATE TABLE `stock_industry_board` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `seq_no` int DEFAULT NULL COMMENT '序号',
  `sector_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '板块名称',
  `change_percent` decimal(8,2) DEFAULT NULL COMMENT '涨跌幅(%)',
  `total_volume` decimal(20,2) DEFAULT NULL COMMENT '总成交量',
  `total_amount` decimal(20,2) DEFAULT NULL COMMENT '总成交额',
  `net_inflow` decimal(20,2) DEFAULT NULL COMMENT '净流入',
  `rise_count` int DEFAULT NULL COMMENT '上涨家数',
  `fall_count` int DEFAULT NULL COMMENT '下跌家数',
  `average_price` decimal(10,2) DEFAULT NULL COMMENT '均价',
  `leading_stock` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '领涨股',
  `leading_stock_price` decimal(10,2) DEFAULT NULL COMMENT '领涨股最新价',
  `leading_stock_change_percent` decimal(8,2) DEFAULT NULL COMMENT '领涨股涨跌幅(%)',
  `trade_date` date DEFAULT NULL COMMENT '交易日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行业板块行情表';

DROP TABLE IF EXISTS `stock_industry_board_em`;
CREATE TABLE `stock_industry_board_em` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `average_price` decimal(38,2) DEFAULT NULL COMMENT '板块均价',
  `change_percent` decimal(38,2) DEFAULT NULL COMMENT '领涨股票涨跌幅(%)',
  `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `fall_count` int DEFAULT NULL COMMENT '下跌家数',
  `leading_stock` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '领涨股票名称',
  `leading_stock_change_percent` decimal(38,2) DEFAULT NULL COMMENT '领涨股票涨跌幅(%)',
  `rise_count` int DEFAULT NULL COMMENT '上涨家数',
  `sector_code` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '行业板块代码（如 BK0475）',
  `sector_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '行业板块名称（如 白酒、半导体）',
  `seq_no` int DEFAULT NULL COMMENT '序号 / 排名',
  `total_volume` decimal(38,4) DEFAULT NULL COMMENT '总成交量(万手)',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '总成交额(亿元)',
  `net_inflow` decimal(38,4) DEFAULT NULL COMMENT '净流入(亿元)',
  `change_amount` decimal(38,4) DEFAULT NULL COMMENT '涨跌额（详情快照）',
  `open_price` decimal(38,4) DEFAULT NULL COMMENT '今开（详情快照）',
  `pre_close_price` decimal(38,4) DEFAULT NULL COMMENT '昨收（详情快照）',
  `high_price` decimal(38,4) DEFAULT NULL COMMENT '最高（详情快照）',
  `low_price` decimal(38,4) DEFAULT NULL COMMENT '最低（详情快照）',
  `turnover_rate` decimal(38,4) DEFAULT NULL COMMENT '换手率(%)（详情快照）',
  `volume_ratio` decimal(38,4) DEFAULT NULL COMMENT '量比（详情快照）',
  `outer_disc` decimal(38,4) DEFAULT NULL COMMENT '外盘(万手，详情快照；内盘=总成交量-外盘)',
  `circulating_market_value` decimal(38,4) DEFAULT NULL COMMENT '流通市值(亿元，详情快照)',
  `circulating_shares` decimal(38,4) DEFAULT NULL COMMENT '流通股本(亿股，详情快照)',
  `detail_trade_date` date DEFAULT NULL COMMENT '详情快照抓取日期',
  `trade_date` date DEFAULT NULL COMMENT '交易日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_board_em_sector_code` (`sector_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='东方财富行业板块当前行情';

DROP TABLE IF EXISTS `stock_industry_board_history`;
CREATE TABLE `stock_industry_board_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `trade_date` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易日期',
  `open_price` decimal(20,4) DEFAULT NULL COMMENT '开盘价',
  `high_price` decimal(20,4) DEFAULT NULL COMMENT '最高价',
  `low_price` decimal(20,4) DEFAULT NULL COMMENT '最低价',
  `close_price` decimal(20,4) DEFAULT NULL COMMENT '收盘价',
  `volume` decimal(20,2) DEFAULT NULL COMMENT '成交量',
  `amount` decimal(20,2) DEFAULT NULL COMMENT '成交额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `sector_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '板块名称',
  `change_amount` decimal(38,2) DEFAULT NULL COMMENT '涨跌额',
  `change_percent` decimal(38,2) DEFAULT NULL COMMENT '涨跌幅（百分比）',
  PRIMARY KEY (`id`),
  KEY `idx_industry_history_trade_date` (`trade_date`),
  KEY `idx_industry_history_sector_date` (`sector_name`,`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行业板块历史行情';

DROP TABLE IF EXISTS `stock_industry_board_history_em`;
CREATE TABLE `stock_industry_board_history_em` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `amount` decimal(38,2) DEFAULT NULL COMMENT '成交额（元）',
  `change_amount` decimal(38,2) DEFAULT NULL COMMENT '涨跌额',
  `change_percent` decimal(38,2) DEFAULT NULL COMMENT '涨跌幅(%)',
  `close_price` decimal(38,2) DEFAULT NULL COMMENT '收盘价',
  `create_time` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `high_price` decimal(38,2) DEFAULT NULL COMMENT '最高价',
  `low_price` decimal(38,2) DEFAULT NULL COMMENT '最低价',
  `open_price` decimal(38,2) DEFAULT NULL COMMENT '开盘价',
  `sector_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '东方财富行业板块名称（如 白酒、半导体）',
  `trade_date` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易日期（格式如 yyyy-MM-dd）',
  `volume` decimal(38,2) DEFAULT NULL COMMENT '成交量（手）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_history_em_sector_date` (`sector_name`,`trade_date`),
  KEY `idx_industry_history_em_trade_date` (`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='东方财富行业板块历史行情';

DROP TABLE IF EXISTS `stock_minute_bar`;
CREATE TABLE `stock_minute_bar` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `bar_time` varchar(19) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时间，格式 yyyy-MM-dd HH:mm:ss',
  `close_price` decimal(38,2) DEFAULT NULL COMMENT '收盘价',
  `code` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票代码，带交易所前缀，如 sh600519',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `high_price` decimal(38,2) DEFAULT NULL COMMENT '最高价',
  `low_price` decimal(38,2) DEFAULT NULL COMMENT '最低价',
  `open_price` decimal(38,2) DEFAULT NULL COMMENT '开盘价',
  `period` int DEFAULT NULL COMMENT '分钟周期，当前固定 1（1分钟K线）',
  `turnover` decimal(38,2) DEFAULT NULL COMMENT '成交额，单位：元',
  `volume` decimal(38,2) DEFAULT NULL COMMENT '成交量，单位：股',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_minute_bar` (`code`,`bar_time`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='股票分钟级K线';

DROP TABLE IF EXISTS `stock_notification`;
CREATE TABLE `stock_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `is_enabled` int DEFAULT NULL COMMENT '是否启用 (1: 是, 0: 否)',
  `last_notify_at` datetime(6) DEFAULT NULL COMMENT '上次提醒时间',
  `params` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '策略参数 (JSON 格式)',
  `stock_code` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标的代码',
  `asset_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'STOCK' COMMENT '标的类型：STOCK-股票，FUND-基金',
  `threshold_value` decimal(38,2) DEFAULT NULL COMMENT '通知阈值价格',
  `type` int DEFAULT NULL COMMENT '提醒类型 (1: 价格/净值通知, 2: 双均线策略, 3: 网格交易, 4: MACD策略)',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `user_id` bigint DEFAULT NULL COMMENT '用户 ID',
  `notify_strategy` int DEFAULT NULL COMMENT '通知策略 (1: 每日一次, 2: 持续重复)',
  PRIMARY KEY (`id`),
  KEY `idx_stock_notification_asset_code` (`asset_type`,`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='股票提醒配置';

DROP TABLE IF EXISTS `stock_performance_report`;
CREATE TABLE `stock_performance_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_date` date NOT NULL COMMENT '报告期',
  `stock_code` varchar(16) NOT NULL COMMENT '股票代码',
  `stock_name` varchar(64) DEFAULT NULL COMMENT '股票名称',
  `earnings_per_share` decimal(18,6) DEFAULT NULL COMMENT '每股收益',
  `total_revenue` decimal(24,4) DEFAULT NULL COMMENT '营业总收入',
  `total_revenue_yoy` decimal(18,6) DEFAULT NULL COMMENT '营业总收入同比增长',
  `total_revenue_qoq` decimal(18,6) DEFAULT NULL COMMENT '营业总收入季度环比增长',
  `net_profit` decimal(24,4) DEFAULT NULL COMMENT '净利润',
  `net_profit_yoy` decimal(18,6) DEFAULT NULL COMMENT '净利润同比增长',
  `net_profit_qoq` decimal(18,6) DEFAULT NULL COMMENT '净利润季度环比增长',
  `net_assets_per_share` decimal(18,6) DEFAULT NULL COMMENT '每股净资产',
  `roe` decimal(18,6) DEFAULT NULL COMMENT '净资产收益率',
  `operating_cash_flow_per_share` decimal(18,6) DEFAULT NULL COMMENT '每股经营现金流量',
  `gross_profit_margin` decimal(18,6) DEFAULT NULL COMMENT '销售毛利率',
  `industry` varchar(128) DEFAULT NULL COMMENT '所处行业',
  `latest_announcement_date` date DEFAULT NULL COMMENT '最新公告日期',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_date_stock_code` (`report_date`,`stock_code`),
  KEY `idx_stock_code` (`stock_code`),
  KEY `idx_latest_announcement_date` (`latest_announcement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票业绩报表';

DROP TABLE IF EXISTS `stock_quote`;
CREATE TABLE `stock_quote` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(16) NOT NULL COMMENT '股票代码，例如 002240 或 bj920000',
  `name` varchar(50) NOT NULL COMMENT '股票名称，例如 安徽凤凰',
  `latest_price` decimal(10,2) DEFAULT NULL COMMENT '最新价',
  `change_amount` decimal(10,2) DEFAULT NULL COMMENT '涨跌额',
  `change_percent` decimal(8,3) DEFAULT NULL COMMENT '涨跌幅(%)',
  `buy_price` decimal(10,2) DEFAULT NULL COMMENT '买入价',
  `sell_price` decimal(10,2) DEFAULT NULL COMMENT '卖出价',
  `prev_close` decimal(10,2) DEFAULT NULL COMMENT '昨收',
  `open_price` decimal(10,2) DEFAULT NULL COMMENT '今开',
  `high_price` decimal(10,2) DEFAULT NULL COMMENT '最高',
  `low_price` decimal(10,2) DEFAULT NULL COMMENT '最低',
  `volume` decimal(20,2) DEFAULT NULL COMMENT '成交量(股)',
  `turnover` decimal(20,2) DEFAULT NULL COMMENT '成交额(元)',
  `quote_time` varchar(20) DEFAULT NULL COMMENT '时间戳，如15:30:01',
  `created_at` datetime DEFAULT NULL,
  `history_hight_price` decimal(10,2) DEFAULT NULL COMMENT '历史最高价',
  `history_low_price` decimal(10,2) DEFAULT NULL COMMENT '历史最低价',
  `pir` decimal(10,2) DEFAULT NULL COMMENT '最新收盘区间位置百分比',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='A股股票最新行情表';

DROP TABLE IF EXISTS `stock_quote_history`;
CREATE TABLE `stock_quote_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(16) NOT NULL COMMENT '股票代码，例如 bj920000',
  `name` varchar(50) NOT NULL COMMENT '股票名称',
  `close_price` decimal(10,2) DEFAULT NULL COMMENT '收盘价',
  `open_price` decimal(10,2) DEFAULT NULL COMMENT '开盘价',
  `high_price` decimal(10,2) DEFAULT NULL COMMENT '最高价',
  `low_price` decimal(10,2) DEFAULT NULL COMMENT '最低价',
  `volume` decimal(20,2) DEFAULT NULL COMMENT '成交量(股)',
  `turnover` decimal(20,2) DEFAULT NULL COMMENT '成交额(元)',
  `quote_time` varchar(20) DEFAULT NULL COMMENT '时间戳，如15:30:01',
  `created_at` datetime DEFAULT NULL,
  `trade_date` varchar(10) DEFAULT NULL COMMENT '交易日期，如2025-12-18',
  PRIMARY KEY (`id`),
  KEY `idx_code_tradedate` (`code`,`trade_date`),
  KEY `idx_tradedate_code` (`trade_date`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='A股股票历史行情表';

DROP TABLE IF EXISTS `stock_share_change`;
CREATE TABLE `stock_share_change` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stock_code` varchar(16) NOT NULL COMMENT '股票代码，带交易所前缀',
  `stock_name` varchar(64) DEFAULT NULL COMMENT '股票名称',
  `market` varchar(64) DEFAULT NULL COMMENT '交易市场',
  `announcement_date` date DEFAULT NULL COMMENT '公告日期',
  `change_date` date DEFAULT NULL COMMENT '变动日期',
  `change_reason` varchar(128) DEFAULT NULL COMMENT '变动原因',
  `total_shares_10k` decimal(24,4) DEFAULT NULL COMMENT '总股本，单位：万股',
  `floating_shares_10k` decimal(24,4) DEFAULT NULL COMMENT '已流通股份，单位：万股',
  `floating_ratio` decimal(18,6) DEFAULT NULL COMMENT '已流通比例，单位：%',
  `restricted_shares_10k` decimal(24,4) DEFAULT NULL COMMENT '流通受限股份，单位：万股',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_change_announcement_reason` (`stock_code`,`change_date`,`announcement_date`,`change_reason`),
  KEY `idx_stock_code` (`stock_code`),
  KEY `idx_change_date` (`change_date`),
  KEY `idx_announcement_date` (`announcement_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票股本变动';

DROP TABLE IF EXISTS `stock_strategy_dual_ma_backtest_snapshot`;
CREATE TABLE `stock_strategy_dual_ma_backtest_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` bigint NOT NULL COMMENT '快照批次号',
  `market` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '市场: sh/sz/bj',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '股票代码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '股票名称',
  `ma_short` int NOT NULL COMMENT '短期均线',
  `ma_long` int NOT NULL COMMENT '长期均线',
  `recent_years` int NOT NULL COMMENT '回测年数',
  `total_return` decimal(20,8) DEFAULT NULL COMMENT '累计收益率',
  `trade_count` int DEFAULT NULL COMMENT '交易次数',
  `win_rate` decimal(20,8) DEFAULT NULL COMMENT '胜率',
  `t_value` double DEFAULT NULL COMMENT 'T统计量',
  `p_value` double DEFAULT NULL COMMENT 'P值',
  `reliability` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可靠性评级',
  `latest_price` decimal(20,8) DEFAULT NULL COMMENT '最新价',
  `pir` decimal(20,8) DEFAULT NULL COMMENT '价格区间指标',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dual_ma_backtest_snapshot` (`batch_no`,`market`,`ma_short`,`ma_long`,`recent_years`,`code`),
  KEY `idx_dual_ma_backtest_snapshot_query` (`batch_no`,`market`,`ma_short`,`ma_long`,`recent_years`),
  KEY `idx_dual_ma_backtest_snapshot_code` (`batch_no`,`market`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='双均线策略回测快照表';

DROP TABLE IF EXISTS `stock_strategy_grid_backtest_snapshot`;
CREATE TABLE `stock_strategy_grid_backtest_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `batch_no` bigint DEFAULT NULL COMMENT '回测批次号',
  `code` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票代码（如 600519、000001）',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `grid_count` int DEFAULT NULL COMMENT '单方向允许交易的最大网格层数',
  `grid_rate` decimal(38,2) DEFAULT NULL COMMENT '单格价格涨跌比例（如 0.03 表示 3%）',
  `latest_price` decimal(38,2) DEFAULT NULL COMMENT '最新股票价格',
  `market` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属市场（如 A股/主板/创业板等）',
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票名称',
  `p_value` double DEFAULT NULL COMMENT 'P 值（统计显著性指标，P < 0.05 具有显著性）',
  `pir` decimal(38,2) DEFAULT NULL COMMENT '价格处于历史区间比例（PIR）',
  `recent_years` int DEFAULT NULL COMMENT '回测年限（如 1、3、5 年）',
  `reliability` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '策略可靠性评级',
  `t_value` double DEFAULT NULL COMMENT 'T 检验统计量',
  `total_return` decimal(38,2) DEFAULT NULL COMMENT '策略累计收益率（百分比）',
  `trade_count` int DEFAULT NULL COMMENT '交易次数（开平仓交易总笔数）',
  `win_rate` decimal(38,2) DEFAULT NULL COMMENT '胜率（百分比）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='网格交易策略历史回测快照';

DROP TABLE IF EXISTS `stock_strategy_macd_backtest_snapshot`;
CREATE TABLE `stock_strategy_macd_backtest_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `batch_no` bigint DEFAULT NULL COMMENT '回测批次号',
  `code` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票代码（如 600519、000001）',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `fast_period` int DEFAULT NULL COMMENT '快线 EMA 周期（通常为 12）',
  `latest_price` decimal(38,2) DEFAULT NULL COMMENT '最新价',
  `market` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属市场（如 A股/主板/创业板等）',
  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '股票名称',
  `p_value` double DEFAULT NULL COMMENT 'P 值（统计显著性指标，P < 0.05 具有显著性）',
  `pir` decimal(38,2) DEFAULT NULL COMMENT '价格处于历史区间比例（PIR）',
  `recent_years` int DEFAULT NULL COMMENT '回测年限（如 1、3、5 年）',
  `reliability` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '策略可靠性评级',
  `signal_period` int DEFAULT NULL COMMENT '信号线 DEA 平滑周期（通常为 9）',
  `slow_period` int DEFAULT NULL COMMENT '慢线 EMA 周期（通常为 26）',
  `t_value` double DEFAULT NULL COMMENT 'T 检验统计量',
  `total_return` decimal(38,2) DEFAULT NULL COMMENT '策略累计收益率（百分比）',
  `trade_count` int DEFAULT NULL COMMENT '交易次数（开平仓交易总笔数）',
  `win_rate` decimal(38,2) DEFAULT NULL COMMENT '胜率（百分比）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MACD 策略历史回测快照';

DROP TABLE IF EXISTS `stock_strategy_momentum_backtest_snapshot`;
CREATE TABLE `stock_strategy_momentum_backtest_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` bigint NOT NULL COMMENT '快照批次号',
  `market` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '市场: sh/sz/bj',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '股票代码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '股票名称',
  `lookback_days` int NOT NULL COMMENT '回望天数',
  `recent_years` int NOT NULL COMMENT '回测年数',
  `total_return` decimal(20,8) DEFAULT NULL COMMENT '累计收益率',
  `trade_count` int DEFAULT NULL COMMENT '交易次数',
  `win_rate` decimal(20,8) DEFAULT NULL COMMENT '胜率',
  `t_value` double DEFAULT NULL COMMENT 'T统计量',
  `p_value` double DEFAULT NULL COMMENT 'P值',
  `reliability` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可靠性评级',
  `latest_price` decimal(20,8) DEFAULT NULL COMMENT '最新价',
  `pir` decimal(20,8) DEFAULT NULL COMMENT '价格区间指标',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_momentum_backtest_snapshot` (`batch_no`,`market`,`lookback_days`,`recent_years`,`code`),
  KEY `idx_momentum_backtest_snapshot_query` (`batch_no`,`market`,`lookback_days`,`recent_years`),
  KEY `idx_momentum_backtest_snapshot_code` (`batch_no`,`market`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动量策略回测快照表';

DROP TABLE IF EXISTS `stock_sync`;
CREATE TABLE `stock_sync` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `value` varchar(64) DEFAULT NULL COMMENT '值',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票同步配置';

DROP TABLE IF EXISTS `stock_trade_calendar`;
CREATE TABLE `stock_trade_calendar` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trade_date` varchar(10) NOT NULL COMMENT '非交易日日期，如 2025-10-01',
  `market` varchar(10) NOT NULL DEFAULT 'A' COMMENT '市场类型：A-沪深京',
  `remark` varchar(100) DEFAULT NULL COMMENT '备注，如 国庆节 / 春节',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_trade_calendar_market_date` (`market`,`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票非交易日历（仅记录非交易日）';

DROP TABLE IF EXISTS `stock_valuation_metrics`;
CREATE TABLE `stock_valuation_metrics` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stock_code` varchar(16) NOT NULL COMMENT '股票代码',
  `stock_name` varchar(64) NOT NULL COMMENT '股票简称',
  `peg` decimal(20,10) DEFAULT NULL COMMENT 'PEG值',
  `peg_industry_med` decimal(20,10) DEFAULT NULL COMMENT 'PEG值-行业中值',
  `peg_industry_avg` decimal(20,10) DEFAULT NULL COMMENT 'PEG值-行业平均',
  `peg_rank` decimal(20,10) DEFAULT NULL COMMENT 'PEG排名',
  `pe_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '市盈率(去年实际)',
  `pe_last_y_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市盈率(去年实际)-行业中值',
  `pe_last_y_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市盈率(去年实际)-行业平均',
  `pe_ttm` decimal(20,10) DEFAULT NULL COMMENT '市盈率(TTM)',
  `pe_ttm_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市盈率(TTM)-行业中值',
  `pe_ttm_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市盈率(TTM)-行业平均',
  `pe_this_y_e` decimal(20,10) DEFAULT NULL COMMENT '市盈率(今年预测)',
  `pe_this_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市盈率(今年预测)-行业中值',
  `pe_this_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市盈率(今年预测)-行业平均',
  `pe_next_y_e` decimal(20,10) DEFAULT NULL COMMENT '市盈率(明年预测)',
  `pe_next_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市盈率(明年预测)-行业中值',
  `pe_next_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市盈率(明年预测)-行业平均',
  `pe_next_2y_e` decimal(20,10) DEFAULT NULL COMMENT '市盈率(后年预测)',
  `pe_next_2y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市盈率(后年预测)-行业中值',
  `pe_next_2y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市盈率(后年预测)-行业平均',
  `ps_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '市销率(去年实际)',
  `ps_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市销率(去年实际)-行业中值',
  `ps_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市销率(去年实际)-行业平均',
  `ps_ttm` decimal(20,10) DEFAULT NULL COMMENT '市销率(TTM)',
  `ps_ttm_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市销率(TTM)-行业中值',
  `ps_ttm_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市销率(TTM)-行业平均',
  `ps_this_y_e` decimal(20,10) DEFAULT NULL COMMENT '市销率(今年预测)',
  `ps_this_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市销率(今年预测)-行业中值',
  `ps_this_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市销率(今年预测)-行业平均',
  `ps_next_y_e` decimal(20,10) DEFAULT NULL COMMENT '市销率(明年预测)',
  `ps_next_y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市销率(明年预测)-行业中值',
  `ps_next_y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市销率(明年预测)-行业平均',
  `ps_next_2y_e` decimal(20,10) DEFAULT NULL COMMENT '市销率(后年预测)',
  `ps_next_2y_e_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市销率(后年预测)-行业中值',
  `ps_next_2y_e_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市销率(后年预测)-行业平均',
  `pb_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '市净率(去年实际)',
  `pb_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市净率(去年实际)-行业中值',
  `pb_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市净率(去年实际)-行业平均',
  `pb_mrq` decimal(20,10) DEFAULT NULL COMMENT '市净率(MRQ)',
  `pb_mrq_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市净率(MRQ)-行业中值',
  `pb_mrq_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市净率(MRQ)-行业平均',
  `pce_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '市现率PCE(去年实际)',
  `pce_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市现率PCE(去年实际)-行业中值',
  `pce_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市现率PCE(去年实际)-行业平均',
  `pce_ttm` decimal(20,10) DEFAULT NULL COMMENT '市现率PCE(TTM)',
  `pce_ttm_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市现率PCE(TTM)-行业中值',
  `pce_ttm_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市现率PCE(TTM)-行业平均',
  `pcf_last_y_a` decimal(20,10) DEFAULT NULL COMMENT '市现率PCF(去年实际)',
  `pcf_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市现率PCF(去年实际)-行业中值',
  `pcf_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市现率PCF(去年实际)-行业平均',
  `pcf_ttm` decimal(20,10) DEFAULT NULL COMMENT '市现率PCF(TTM)',
  `pcf_ttm_industry_med` decimal(20,10) DEFAULT NULL COMMENT '市现率PCF(TTM)-行业中值',
  `pcf_ttm_industry_avg` decimal(20,10) DEFAULT NULL COMMENT '市现率PCF(TTM)-行业平均',
  `ev_ebitda_last_y_a` decimal(20,10) DEFAULT NULL COMMENT 'EV/EBITDA(去年实际)',
  `ev_ebitda_last_y_a_industry_med` decimal(20,10) DEFAULT NULL COMMENT 'EV/EBITDA(去年实际)-行业中值',
  `ev_ebitda_last_y_a_industry_avg` decimal(20,10) DEFAULT NULL COMMENT 'EV/EBITDA(去年实际)-行业平均',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `conclusion` varchar(500) DEFAULT NULL COMMENT '估值结论',
  `industry` varchar(255) DEFAULT NULL COMMENT '所属行业',
  `net_profit_ttm` decimal(38,2) DEFAULT NULL COMMENT '归母净利润 TTM (元)',
  `pe_last_2y_a` decimal(38,2) DEFAULT NULL COMMENT '市盈率(2年前实际)',
  `pe_last_3y_a` decimal(38,2) DEFAULT NULL COMMENT '市盈率(3年前实际)',
  `total_market_cap` decimal(38,2) DEFAULT NULL COMMENT '总市值 (元)',
  `valuation_level` varchar(255) DEFAULT NULL COMMENT '估值等级 (低估, 偏低估, 合理偏低, 合理偏高, 偏高估, 高估)',
  `valuation_score` decimal(38,2) DEFAULT NULL COMMENT '估值评分 (0~100)',
  PRIMARY KEY (`id`),
  KEY `idx_stock_code` (`stock_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='股票估值指标表';

DROP TABLE IF EXISTS `stock_watchlist_group`;
CREATE TABLE `stock_watchlist_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `name` varchar(100) NOT NULL COMMENT '分组名称',
  `sort_no` int DEFAULT '0' COMMENT '排序号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `type` varchar(20) NOT NULL DEFAULT 'STOCK' COMMENT '分组类型: STOCK-股票自选组, FUND-基金自选组',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自选股票分组表';

DROP TABLE IF EXISTS `stock_watchlist_stock`;
CREATE TABLE `stock_watchlist_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '分组ID',
  `stock_code` varchar(20) NOT NULL COMMENT '股票代码',
  `sort_no` int DEFAULT '0' COMMENT '排序号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_stock` (`group_id`,`stock_code`) COMMENT '同一分组下股票不重复',
  KEY `idx_groupid` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自选股票关联表';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `nickname` varchar(100) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1=启用, 0=禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

DROP TABLE IF EXISTS `user_article`;
CREATE TABLE `user_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章内容',
  `author_id` bigint NOT NULL COMMENT '作者用户ID',
  `author_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作者用户名',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `visibility` tinyint NOT NULL DEFAULT '0' COMMENT '可见性: 0=私密, 1=公开',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_visibility` (`visibility`),
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户文章表';

DROP TABLE IF EXISTS `user_portfolio`;
CREATE TABLE `user_portfolio` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(100) NOT NULL COMMENT '投资组合名称',
  `base_currency` varchar(10) NOT NULL DEFAULT 'CNY' COMMENT '组合基础币种',
  `benchmark_code` varchar(20) DEFAULT NULL COMMENT '业绩比较基准代码',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为默认组合',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_portfolio_user` (`user_id`,`deleted`),
  KEY `idx_user_portfolio_name` (`user_id`,`name`,`deleted`),
  CONSTRAINT `fk_user_portfolio_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户投资组合';

DROP TABLE IF EXISTS `user_broker_account`;
CREATE TABLE `user_broker_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `portfolio_id` bigint NOT NULL COMMENT '投资组合ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，用于直接校验账户归属',
  `account_name` varchar(100) NOT NULL COMMENT '账户名称',
  `broker_code` varchar(50) NOT NULL COMMENT '券商代码',
  `broker_name` varchar(100) DEFAULT NULL COMMENT '券商名称',
  `account_no_masked` varchar(100) DEFAULT NULL COMMENT '脱敏账户号码',
  `account_no_hash` char(64) DEFAULT NULL COMMENT '用户、券商和账户号码的SHA-256摘要',
  `account_type` varchar(30) NOT NULL DEFAULT 'SECURITIES' COMMENT '账户类型',
  `sync_mode` varchar(30) NOT NULL DEFAULT 'MANUAL' COMMENT '同步方式',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_broker_account_hash` (`user_id`,`broker_code`,`account_no_hash`),
  KEY `idx_broker_account_portfolio` (`portfolio_id`,`deleted`),
  CONSTRAINT `fk_broker_account_portfolio` FOREIGN KEY (`portfolio_id`) REFERENCES `user_portfolio` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_broker_account_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户券商账户';

DROP TABLE IF EXISTS `user_portfolio_import_batch`;
CREATE TABLE `user_portfolio_import_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '券商账户ID',
  `source` varchar(30) NOT NULL COMMENT '数据来源',
  `source_file_name` varchar(255) DEFAULT NULL COMMENT '来源文件名',
  `source_file_hash` char(64) DEFAULT NULL COMMENT '来源文件摘要',
  `status` varchar(20) NOT NULL COMMENT '批次状态',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总记录数',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功数',
  `skip_count` int NOT NULL DEFAULT '0' COMMENT '重复跳过数',
  `failure_count` int NOT NULL DEFAULT '0' COMMENT '失败数',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '失败信息',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_portfolio_import_account` (`account_id`,`create_time`),
  KEY `idx_portfolio_import_file` (`account_id`,`source_file_hash`),
  CONSTRAINT `fk_portfolio_import_account` FOREIGN KEY (`account_id`) REFERENCES `user_broker_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='持仓交易导入批次';

DROP TABLE IF EXISTS `user_portfolio_trade`;
CREATE TABLE `user_portfolio_trade` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '券商账户ID',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `asset_type` varchar(20) NOT NULL COMMENT '资产类型',
  `market` varchar(10) DEFAULT NULL COMMENT '交易市场',
  `asset_code` varchar(30) DEFAULT NULL COMMENT '系统标准资产代码',
  `asset_name` varchar(100) DEFAULT NULL COMMENT '资产名称快照',
  `trade_type` varchar(30) NOT NULL COMMENT '流水类型',
  `trade_time` datetime NOT NULL COMMENT '交易时间',
  `settlement_date` date DEFAULT NULL COMMENT '交收日期',
  `quantity` decimal(24,8) DEFAULT NULL COMMENT '成交数量',
  `price` decimal(24,8) DEFAULT NULL COMMENT '成交价格',
  `gross_amount` decimal(24,4) DEFAULT NULL COMMENT '成交总额',
  `commission` decimal(24,4) NOT NULL DEFAULT '0' COMMENT '佣金',
  `stamp_duty` decimal(24,4) NOT NULL DEFAULT '0' COMMENT '印花税',
  `transfer_fee` decimal(24,4) NOT NULL DEFAULT '0' COMMENT '过户费',
  `other_fee` decimal(24,4) NOT NULL DEFAULT '0' COMMENT '其他费用',
  `net_amount` decimal(24,4) DEFAULT NULL COMMENT '对现金的净影响金额',
  `currency` varchar(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `source` varchar(30) NOT NULL COMMENT '数据来源',
  `source_trade_id` varchar(100) DEFAULT NULL COMMENT '来源系统流水号',
  `dedup_key` char(64) NOT NULL COMMENT '流水幂等摘要',
  `status` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL或REVERSED',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portfolio_trade_dedup` (`account_id`,`dedup_key`),
  KEY `idx_portfolio_trade_time` (`account_id`,`trade_time`),
  KEY `idx_portfolio_trade_asset` (`account_id`,`asset_type`,`asset_code`),
  KEY `idx_portfolio_trade_batch` (`import_batch_id`),
  CONSTRAINT `fk_portfolio_trade_account` FOREIGN KEY (`account_id`) REFERENCES `user_broker_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_portfolio_trade_batch` FOREIGN KEY (`import_batch_id`) REFERENCES `user_portfolio_import_batch` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户投资组合交易流水';

DROP TABLE IF EXISTS `user_portfolio_position`;
CREATE TABLE `user_portfolio_position` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '券商账户ID',
  `asset_type` varchar(20) NOT NULL COMMENT '资产类型',
  `market` varchar(10) DEFAULT NULL COMMENT '交易市场',
  `asset_code` varchar(30) NOT NULL COMMENT '系统标准资产代码',
  `asset_name` varchar(100) DEFAULT NULL COMMENT '资产名称',
  `currency` varchar(10) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `quantity` decimal(24,8) NOT NULL COMMENT '持仓数量',
  `available_quantity` decimal(24,8) DEFAULT NULL COMMENT '可用数量',
  `cost_price` decimal(24,8) DEFAULT NULL COMMENT '平均成本价',
  `cost_amount` decimal(24,4) DEFAULT NULL COMMENT '持仓成本金额',
  `latest_price` decimal(24,8) DEFAULT NULL COMMENT '最新价格或净值',
  `market_value` decimal(24,4) DEFAULT NULL COMMENT '最新市值',
  `unrealized_profit` decimal(24,4) DEFAULT NULL COMMENT '未实现盈亏',
  `quote_date` date DEFAULT NULL COMMENT '行情或净值日期',
  `calculate_time` datetime NOT NULL COMMENT '计算时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portfolio_position_asset` (`account_id`,`asset_type`,`asset_code`),
  KEY `idx_portfolio_position_value` (`account_id`,`market_value`),
  CONSTRAINT `fk_portfolio_position_account` FOREIGN KEY (`account_id`) REFERENCES `user_broker_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户当前持仓';

DROP TABLE IF EXISTS `user_portfolio_cash`;
CREATE TABLE `user_portfolio_cash` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '券商账户ID',
  `currency` varchar(10) NOT NULL COMMENT '币种',
  `total_balance` decimal(24,4) NOT NULL COMMENT '现金总余额',
  `available_balance` decimal(24,4) DEFAULT NULL COMMENT '可用余额',
  `frozen_balance` decimal(24,4) DEFAULT NULL COMMENT '冻结余额',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portfolio_cash_currency` (`account_id`,`currency`),
  CONSTRAINT `fk_portfolio_cash_account` FOREIGN KEY (`account_id`) REFERENCES `user_broker_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户当前现金';

DROP TABLE IF EXISTS `user_portfolio_account_snapshot`;
CREATE TABLE `user_portfolio_account_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '券商账户ID',
  `snapshot_date` date NOT NULL COMMENT '快照日期',
  `currency` varchar(10) NOT NULL COMMENT '汇总币种',
  `cash_amount` decimal(24,4) NOT NULL COMMENT '现金余额',
  `market_value` decimal(24,4) NOT NULL COMMENT '持仓市值',
  `total_asset` decimal(24,4) NOT NULL COMMENT '总资产',
  `cost_amount` decimal(24,4) NOT NULL COMMENT '持仓成本',
  `unrealized_profit` decimal(24,4) NOT NULL COMMENT '未实现盈亏',
  `unpriced_asset_count` int NOT NULL DEFAULT '0' COMMENT '缺少行情的资产数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portfolio_account_snapshot` (`account_id`,`snapshot_date`),
  KEY `idx_portfolio_snapshot_date` (`snapshot_date`),
  CONSTRAINT `fk_portfolio_snapshot_account` FOREIGN KEY (`account_id`) REFERENCES `user_broker_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户每日资产快照';
