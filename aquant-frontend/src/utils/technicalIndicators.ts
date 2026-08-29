export interface TechnicalHistoryPoint {
  tradeDate: string;
  openPrice: number;
  closePrice: number;
  lowPrice: number;
  highPrice: number;
  volume: number;
}

export type IndicatorValue = number | '-';

export interface IndicatorVisibility {
  macd: boolean;
  kdj: boolean;
  boll: boolean;
}

export interface IndicatorGridLayout {
  top: string;
  height: string;
}

export interface TechnicalChartLayout {
  subIndicatorCount: number;
  mainGridHeight: string;
  volumeGridTop: string;
  volumeGridHeight: string;
  macdGrid: IndicatorGridLayout;
  kdjGrid: IndicatorGridLayout;
  bollGrid: IndicatorGridLayout;
  showMacdDates: boolean;
  showKdjDates: boolean;
}

const roundIndicatorValue = (value: number, digits = 4): number => {
  return +value.toFixed(digits);
};

export const calculateMA = (dayCount: number, data: TechnicalHistoryPoint[]): IndicatorValue[] => {
  const result: IndicatorValue[] = [];
  for (let index = 0; index < data.length; index += 1) {
    if (index < dayCount - 1) {
      result.push('-');
      continue;
    }

    let sum = 0;
    for (let offset = 0; offset < dayCount; offset += 1) {
      sum += data[index - offset]!.closePrice;
    }
    result.push(roundIndicatorValue(sum / dayCount, 2));
  }
  return result;
};

export const calculateMACD = (data: TechnicalHistoryPoint[]) => {
  const dif: IndicatorValue[] = [];
  const dea: IndicatorValue[] = [];
  const macd: IndicatorValue[] = [];
  let ema12: number | undefined;
  let ema26: number | undefined;
  let deaValue: number | undefined;

  data.forEach(item => {
    const close = item.closePrice;
    ema12 = ema12 === undefined ? close : close * (2 / 13) + ema12 * (11 / 13);
    ema26 = ema26 === undefined ? close : close * (2 / 27) + ema26 * (25 / 27);
    const difValue = ema12 - ema26;
    deaValue = deaValue === undefined ? difValue : difValue * (2 / 10) + deaValue * (8 / 10);
    dif.push(roundIndicatorValue(difValue));
    dea.push(roundIndicatorValue(deaValue));
    macd.push(roundIndicatorValue((difValue - deaValue) * 2));
  });

  return { dif, dea, macd };
};

export const calculateKDJ = (data: TechnicalHistoryPoint[]) => {
  const k: IndicatorValue[] = [];
  const d: IndicatorValue[] = [];
  const j: IndicatorValue[] = [];
  let kValue = 50;
  let dValue = 50;

  data.forEach((item, index) => {
    if (index < 8) {
      k.push('-');
      d.push('-');
      j.push('-');
      return;
    }

    const window = data.slice(index - 8, index + 1);
    const highestHigh = Math.max(...window.map(value => value.highPrice));
    const lowestLow = Math.min(...window.map(value => value.lowPrice));
    const rsv = highestHigh === lowestLow
      ? 50
      : ((item.closePrice - lowestLow) / (highestHigh - lowestLow)) * 100;
    kValue = (2 * kValue + rsv) / 3;
    dValue = (2 * dValue + kValue) / 3;
    const jValue = 3 * kValue - 2 * dValue;
    k.push(roundIndicatorValue(kValue, 2));
    d.push(roundIndicatorValue(dValue, 2));
    j.push(roundIndicatorValue(jValue, 2));
  });

  return { k, d, j };
};

export const calculateBollingerBands = (data: TechnicalHistoryPoint[]) => {
  const upper: IndicatorValue[] = [];
  const middle: IndicatorValue[] = [];
  const lower: IndicatorValue[] = [];

  for (let index = 0; index < data.length; index += 1) {
    if (index < 19) {
      upper.push('-');
      middle.push('-');
      lower.push('-');
      continue;
    }

    const closes = data.slice(index - 19, index + 1).map(value => value.closePrice);
    const average = closes.reduce((sum, close) => sum + close, 0) / closes.length;
    const variance = closes.reduce((sum, close) => sum + (close - average) ** 2, 0) / closes.length;
    const deviation = Math.sqrt(variance);
    middle.push(roundIndicatorValue(average, 2));
    upper.push(roundIndicatorValue(average + 2 * deviation, 2));
    lower.push(roundIndicatorValue(average - 2 * deviation, 2));
  }

  return { upper, middle, lower };
};

export const getTechnicalChartLayout = (visibility: IndicatorVisibility): TechnicalChartLayout => {
  const subIndicatorCount = Number(visibility.macd) + Number(visibility.kdj) + Number(visibility.boll);
  const mainGridHeight = subIndicatorCount === 0 ? '65%'
    : subIndicatorCount === 1 ? '47%'
      : subIndicatorCount === 2 ? '39%'
        : '31%';
  const volumeGridTop = subIndicatorCount === 0 ? '78%'
    : subIndicatorCount === 1 ? '59%'
      : subIndicatorCount === 2 ? '50%'
        : '43%';
  const volumeGridHeight = subIndicatorCount === 3 ? '9%' : '11%';
  const subGridTops = subIndicatorCount === 1 ? ['74%']
    : subIndicatorCount === 2 ? ['64%', '81%']
      : ['55%', '69%', '83%'];
  const subGridHeight = subIndicatorCount === 1 ? '16%'
    : subIndicatorCount === 2 ? '14%'
      : '11%';
  let visibleSubGridIndex = 0;

  const getSubGridLayout = (visible: boolean): IndicatorGridLayout => {
    if (!visible) {
      return { top: '0%', height: '0%' };
    }
    const top = subGridTops[visibleSubGridIndex] ?? '0%';
    visibleSubGridIndex += 1;
    return { top, height: subGridHeight };
  };

  return {
    subIndicatorCount,
    mainGridHeight,
    volumeGridTop,
    volumeGridHeight,
    macdGrid: getSubGridLayout(visibility.macd),
    kdjGrid: getSubGridLayout(visibility.kdj),
    bollGrid: getSubGridLayout(visibility.boll),
    showMacdDates: visibility.macd && !visibility.kdj && !visibility.boll,
    showKdjDates: visibility.kdj && !visibility.boll
  };
};
