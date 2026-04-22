/** 工作台级常量，提供默认账号、默认表单值和固定分组选项。 */
import type { SipCadenceInput, SipWeekdayInput, WatchlistGroup } from '../appTypes'

export const defaultUsername = 'demo_analyst'
export const defaultPassword = 'ChangeMe123!'
export const defaultSipCadence: SipCadenceInput = 'DAILY'
export const defaultSipWeekday: SipWeekdayInput = '1'
export const defaultSipMonthDay = '1'
export const defaultWatchlistGroupName: WatchlistGroup = '全部'

export function createDefaultWatchlistGroup(): WatchlistGroup {
  return defaultWatchlistGroupName
}
