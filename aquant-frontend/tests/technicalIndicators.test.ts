import assert from 'node:assert/strict';
import test from 'node:test';
import {
  calculateBollingerBands,
  calculateKDJ,
  calculateMACD,
  getTechnicalChartLayout,
  type TechnicalHistoryPoint
} from '../src/utils/technicalIndicators.js';

const createPoints = (count: number, closePrice = 10): TechnicalHistoryPoint[] => {
  return Array.from({ length: count }, (_, index) => ({
    tradeDate: `2026-01-${String(index + 1).padStart(2, '0')}`,
    openPrice: closePrice,
    closePrice,
    lowPrice: closePrice,
    highPrice: closePrice,
    volume: 1000 + index
  }));
};

test('MACD remains zero for a flat price series', () => {
  const result = calculateMACD(createPoints(30));

  assert.equal(result.macd.length, 30);
  assert.ok(result.macd.every(value => value === 0));
  assert.ok(result.dif.every(value => value === 0));
  assert.ok(result.dea.every(value => value === 0));
});

test('KDJ starts after nine periods and stays neutral for a flat series', () => {
  const result = calculateKDJ(createPoints(12));

  assert.deepEqual(result.k.slice(0, 8), Array(8).fill('-'));
  assert.deepEqual(result.d.slice(0, 8), Array(8).fill('-'));
  assert.deepEqual(result.j.slice(0, 8), Array(8).fill('-'));
  assert.deepEqual(result.k.slice(8), Array(4).fill(50));
  assert.deepEqual(result.d.slice(8), Array(4).fill(50));
  assert.deepEqual(result.j.slice(8), Array(4).fill(50));
});

test('BOLL starts after twenty periods and collapses to the flat price', () => {
  const result = calculateBollingerBands(createPoints(22));

  assert.deepEqual(result.middle.slice(0, 19), Array(19).fill('-'));
  assert.deepEqual(result.upper.slice(19), Array(3).fill(10));
  assert.deepEqual(result.middle.slice(19), Array(3).fill(10));
  assert.deepEqual(result.lower.slice(19), Array(3).fill(10));
});

test('technical chart layout allocates visible sub-panels in display order', () => {
  const emptyLayout = getTechnicalChartLayout({ macd: false, kdj: false, boll: false });
  assert.equal(emptyLayout.subIndicatorCount, 0);
  assert.equal(emptyLayout.mainGridHeight, '65%');
  assert.equal(emptyLayout.macdGrid.height, '0%');

  const partialLayout = getTechnicalChartLayout({ macd: false, kdj: true, boll: true });
  assert.equal(partialLayout.subIndicatorCount, 2);
  assert.deepEqual(partialLayout.kdjGrid, { top: '64%', height: '14%' });
  assert.deepEqual(partialLayout.bollGrid, { top: '81%', height: '14%' });
  assert.equal(partialLayout.showKdjDates, false);

  const fullLayout = getTechnicalChartLayout({ macd: true, kdj: true, boll: true });
  assert.equal(fullLayout.mainGridHeight, '31%');
  assert.deepEqual(fullLayout.macdGrid, { top: '55%', height: '11%' });
  assert.deepEqual(fullLayout.kdjGrid, { top: '69%', height: '11%' });
  assert.deepEqual(fullLayout.bollGrid, { top: '83%', height: '11%' });
});
