/**
 * 商品相关常量 —— 全项目统一引用，避免各页面成色/状态映射不一致
 */

/**
 * 成色档位（前端 5 档 → 后端 5 档 conditionLevel 映射）
 * 后端 level: 1=全新 / 2=99新(准新) / 3=95成新 / 4=9成新 / 5=战损版(8成新及以下)
 */
export const CONDITIONS = [
  { id: 'NEW', name: '全新', tip: '未拆封或仅拆封未使用', color: 'text-brand-success', level: 1 },
  { id: 'LIKE_NEW', name: '准新', tip: '拆封未使用/近乎零使用痕迹', color: 'text-brand-primary', level: 2 },
  { id: 'VERY_GOOD', name: '95成新', tip: '轻微使用痕迹，功能完好', color: 'text-brand-info', level: 3 },
  { id: 'GOOD', name: '9成新', tip: '明显使用痕迹，不影响功能', color: 'text-brand-warning', level: 4 },
  { id: 'FAIR', name: '8成新及以下', tip: '外观/性能有瑕疵', color: 'text-brand-error', level: 5 }
]

/** 前端成色ID → 后端 conditionLevel */
export const CONDITION_TO_LEVEL = Object.fromEntries(
  CONDITIONS.map(c => [c.id, c.level])
)

/** 后端 conditionLevel → 前端成色ID */
export const LEVEL_TO_CONDITION = {
  1: 'NEW',
  2: 'LIKE_NEW',
  3: 'VERY_GOOD',
  4: 'GOOD',
  5: 'FAIR'
}

/** 前端成色ID → 展示名称 */
export const CONDITION_NAME = Object.fromEntries(
  CONDITIONS.map(c => [c.id, c.name])
)

/**
 * 商品状态（后端 status 数字 → 前端文案/样式）
 * 后端: 0草稿 / 1审核中 / 2在售 / 3已售罄 / 4下架 / 5审核拒绝
 */
export const PRODUCT_STATUS = {
  0: { name: '草稿', class: 'bg-brand-ink-2/15 text-brand-ink-2 border-brand-ink-2/30' },
  1: { name: '审核中', class: 'bg-brand-warning/15 text-brand-warning border-brand-warning/30' },
  2: { name: '在售中', class: 'bg-brand-success/15 text-brand-success border-brand-success/30' },
  3: { name: '已售罄', class: 'bg-brand-ink-2/15 text-brand-ink-2 border-brand-ink-2/30' },
  4: { name: '已下架', class: 'bg-brand-ink-2/15 text-brand-ink-2 border-brand-ink-2/30' },
  5: { name: '审核拒绝', class: 'bg-brand-error/15 text-brand-error border-brand-error/30' }
}

/** 后端 status → 前端 ListingsView 用的 status id */
export const STATUS_TO_LISTING_ID = {
  0: 'draft',
  1: 'auditing',
  2: 'on_sale',
  3: 'sold_out',
  4: 'off_shelf',
  5: 'rejected'
}
