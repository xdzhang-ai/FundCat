/** 基金详情页辅助函数，处理盘中走势 mock 数据和数值字号自适配。 */
import type { FundDetail } from '@fundcat/contracts'

export function buildIntradayTrend(detail: FundDetail, costLine?: number) {
  const intradayMockOffsets = [
    { x: 9.5, time: '09:30', ratio: 0.18 },
    { x: 10, time: '10:00', ratio: 0.34 },
    { x: 10.5, time: '10:30', ratio: 0.56 },
    { x: 11, time: '11:00', ratio: 0.71 },
    { x: 11.5, time: '11:30/13:00', ratio: 0.63 },
    { x: 12, time: '13:30', ratio: 0.88 },
    { x: 12.5, time: '14:00', ratio: 1 },
  ]
  const start = detail.unitNav
  const end = detail.estimatedNav

  const trend = intradayMockOffsets.map((point, index) => {
    const baseline = start + (end - start) * point.ratio
    const wave = Math.sin((index + 1) * 0.9) * Math.max(start * 0.006, 0.0018)
    const pullToCost = costLine ? (costLine - baseline) * 0.018 : 0
    return {
      x: point.x,
      time: point.time,
      value: Number((baseline + wave + pullToCost).toFixed(4)),
      growth: Number((((baseline + wave + pullToCost - detail.unitNav) / detail.unitNav) * 100).toFixed(2)),
    }
  })

  return [
    ...trend,
    {
      x: 13.5,
      time: '15:00',
      value: null,
      growth: null,
    },
  ]
}

export function valueSizeClass(value: string) {
  const length = value.length
  if (length >= 12) return 'text-[1.00rem]'
  if (length >= 10) return 'text-[1.12rem]'
  if (length >= 8) return 'text-[1.24rem]'
  return 'text-[1.58rem]'
}
