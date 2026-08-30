export interface IndustryRiseAnalysisPoint {
  tradeDate: string;
  sectorName: string;
  rank: number;
  changePercent: number | null;
  changeAmount: number | null;
}

export interface IndustryAnalysisCell extends IndustryRiseAnalysisPoint {
  xIndex: number;
  yIndex: number;
}

export interface IndustryAnalysisMatrix {
  dates: string[];
  ranks: string[];
  cells: IndustryAnalysisCell[];
}

export interface IndustryAnalysisViewState {
  startDate: string;
  endDate: string;
  rankLimit: number;
  scrollLeft: number;
  scrollTop: number;
}

export const INDUSTRY_HEAT_SCALE_COLORS = [
  '#FF0000',
  '#FF2020',
  '#FF4040',
  '#FF6060',
  '#FF7A7A',
  '#FF9999',
  '#FFFFFF',
  '#99FF99',
  '#7AFF7A',
  '#60FF60',
  '#40FF40',
  '#20FF20',
  '#00FF00'
] as const;

export const buildIndustryAnalysisStateQuery = (state: IndustryAnalysisViewState) => ({
  analysisStartDate: state.startDate,
  analysisEndDate: state.endDate,
  analysisRankLimit: String(state.rankLimit),
  analysisScrollLeft: String(Math.max(0, Math.round(state.scrollLeft))),
  analysisScrollTop: String(Math.max(0, Math.round(state.scrollTop)))
});

export const parseIndustryAnalysisViewState = (
  query: Record<string, unknown>
): IndustryAnalysisViewState | null => {
  const startDate = typeof query.analysisStartDate === 'string' ? query.analysisStartDate : '';
  const endDate = typeof query.analysisEndDate === 'string' ? query.analysisEndDate : '';
  const rankLimit = Number(query.analysisRankLimit);
  const scrollLeft = Number(query.analysisScrollLeft ?? 0);
  const scrollTop = Number(query.analysisScrollTop ?? 0);
  if (!startDate || !endDate || !Number.isInteger(rankLimit) || rankLimit < 1 || rankLimit > 100) {
    return null;
  }
  return {
    startDate,
    endDate,
    rankLimit,
    scrollLeft: Number.isFinite(scrollLeft) && scrollLeft >= 0 ? scrollLeft : 0,
    scrollTop: Number.isFinite(scrollTop) && scrollTop >= 0 ? scrollTop : 0
  };
};

export const buildIndustryAnalysisMatrix = (
  points: IndustryRiseAnalysisPoint[],
  maxTradingDays = 10,
  maxRank = 20
): IndustryAnalysisMatrix => {
  const dates = [...new Set(points.map(point => point.tradeDate))]
    .sort((left, right) => right.localeCompare(left))
    .slice(0, Math.max(1, maxTradingDays));
  const dateIndexes = new Map(dates.map((date, index) => [date, index]));
  const cells = points
    .filter(point => dateIndexes.has(point.tradeDate) && point.rank > 0 && point.rank <= maxRank)
    .sort((left, right) => {
      const dateOrder = dateIndexes.get(left.tradeDate)! - dateIndexes.get(right.tradeDate)!;
      return dateOrder !== 0 ? dateOrder : left.rank - right.rank;
    })
    .map(point => ({
      ...point,
      xIndex: dateIndexes.get(point.tradeDate)!,
      yIndex: point.rank - 1
    }));
  const visibleRankCount = cells.reduce((current, cell) => Math.max(current, cell.rank), 0);

  return {
    dates,
    ranks: Array.from({ length: visibleRankCount }, (_, index) => String(index + 1)),
    cells
  };
};

export const formatSignedValue = (value: number | null, suffix: string): string => {
  if (value == null || !Number.isFinite(value)) {
    return '暂无数据';
  }
  const prefix = value > 0 ? '+' : '';
  return `${prefix}${value.toFixed(2)}${suffix}`;
};

export const getIndustryCellColor = (changePercent: number | null): string => {
  if (changePercent == null || !Number.isFinite(changePercent)) {
    return '#F8FAFC';
  }
  const direction = Math.sign(changePercent);
  const level = Math.min(6, Math.floor(Math.abs(changePercent)));
  const colorIndex = 6 - direction * level;
  return INDUSTRY_HEAT_SCALE_COLORS[colorIndex]!;
};
