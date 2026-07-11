type UnitRule = {
  threshold: number
  divisor: number
  suffix: string
}

export const formatUnitValue = (
  value: number | null | undefined,
  units: UnitRule[],
  fallbackSuffix = ''
) => {
  if (value == null) {
    return '-'
  }

  const absValue = Math.abs(value)
  const unit = units.find(item => absValue >= item.threshold)
  if (!unit) {
    return `${value.toLocaleString('zh-CN')}${fallbackSuffix}`
  }

  return `${(value / unit.divisor).toFixed(2).replace(/\.?0+$/, '')}${unit.suffix}`
}

export const formatAmount = (value: number | null | undefined) => {
  return formatUnitValue(value, [
    { threshold: 100000000, divisor: 100000000, suffix: '亿' },
    { threshold: 10000, divisor: 10000, suffix: '万' },
    { threshold: 1000, divisor: 1000, suffix: '千' },
  ])
}

export const formatVolume = (value: number | null | undefined) => {
  return formatUnitValue(value, [
    { threshold: 100000000, divisor: 100000000, suffix: '亿股' },
    { threshold: 10000, divisor: 10000, suffix: '万股' },
  ], '股')
}

export const formatCurrencyAmount = (value: number | null | undefined) => {
  return formatUnitValue(value, [
    { threshold: 100000000, divisor: 100000000, suffix: '亿元' },
    { threshold: 10000, divisor: 10000, suffix: '万元' },
  ], '元')
}
