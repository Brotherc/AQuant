import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildIndustryAnalysisMatrix,
  buildIndustryAnalysisStateQuery,
  formatSignedValue,
  getIndustryCellColor,
  INDUSTRY_HEAT_SCALE_COLORS,
  parseIndustryAnalysisViewState,
  type IndustryRiseAnalysisPoint
} from '../src/utils/industryAnalysis.js';

const point = (
  tradeDate: string,
  sectorName: string,
  rank: number,
  changePercent: number | null
): IndustryRiseAnalysisPoint => ({
  tradeDate,
  sectorName,
  rank,
  changePercent,
  changeAmount: changePercent
});

test('matrix keeps the latest trading days in descending order and maps rank one to the first row', () => {
  const points = [
    point('2026-08-25', '银行', 1, 1),
    point('2026-08-26', '煤炭', 2, 0.5),
    point('2026-08-27', '软件', 1, 2),
    point('2026-08-28', '保险', 3, -1),
    point('2026-08-28', '排名外行业', 21, 5)
  ];

  const matrix = buildIndustryAnalysisMatrix(points, 3);

  assert.deepEqual(matrix.dates, ['2026-08-28', '2026-08-27', '2026-08-26']);
  assert.deepEqual(matrix.ranks, ['1', '2', '3']);
  assert.deepEqual(
    matrix.cells.map(cell => [cell.sectorName, cell.xIndex, cell.yIndex]),
    [['保险', 0, 2], ['软件', 1, 0], ['煤炭', 2, 1]]
  );
});

test('formatters distinguish positive, negative and missing values', () => {
  assert.equal(formatSignedValue(1.2, '%'), '+1.20%');
  assert.equal(formatSignedValue(-0.5, '元'), '-0.50元');
  assert.equal(formatSignedValue(null, '%'), '暂无数据');
});

test('matrix applies the selected rank limit', () => {
  const points = [
    point('2026-08-28', '行业20', 20, 1),
    point('2026-08-28', '行业21', 21, 0.9),
    point('2026-08-28', '行业30', 30, 0.1)
  ];

  const matrix = buildIndustryAnalysisMatrix(points, 10, 30);

  assert.deepEqual(matrix.cells.map(cell => cell.rank), [20, 21, 30]);
  assert.equal(matrix.ranks.length, 30);
});

test('cell colors use thirteen one-percent levels with stronger contrast around zero', () => {
  assert.equal(INDUSTRY_HEAT_SCALE_COLORS.length, 13);
  assert.equal(getIndustryCellColor(12), '#FF0000');
  assert.equal(getIndustryCellColor(6), '#FF0000');
  assert.equal(getIndustryCellColor(5.99), '#FF2020');
  assert.equal(getIndustryCellColor(4), '#FF4040');
  assert.equal(getIndustryCellColor(2), '#FF7A7A');
  assert.equal(getIndustryCellColor(1), '#FF9999');
  assert.equal(getIndustryCellColor(0.99), '#FFFFFF');
  assert.equal(getIndustryCellColor(0), '#FFFFFF');
  assert.equal(getIndustryCellColor(-0.99), '#FFFFFF');
  assert.equal(getIndustryCellColor(-1), '#99FF99');
  assert.equal(getIndustryCellColor(-2), '#7AFF7A');
  assert.equal(getIndustryCellColor(-4), '#40FF40');
  assert.equal(getIndustryCellColor(-5.99), '#20FF20');
  assert.equal(getIndustryCellColor(-6), '#00FF00');
  assert.equal(getIndustryCellColor(-12), '#00FF00');
  assert.equal(getIndustryCellColor(null), '#F8FAFC');
});

test('analysis view state round-trips through route query values', () => {
  const query = buildIndustryAnalysisStateQuery({
    startDate: '2026-08-17',
    endDate: '2026-08-28',
    rankLimit: 30,
    scrollLeft: 320.4,
    scrollTop: 815.8
  });

  assert.deepEqual(parseIndustryAnalysisViewState(query), {
    startDate: '2026-08-17',
    endDate: '2026-08-28',
    rankLimit: 30,
    scrollLeft: 320,
    scrollTop: 816
  });
});
