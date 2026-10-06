const LABELS: Record<string, string> = {
  booster: '補充包',
  starter: '起始牌組',
  promo: '促銷卡',
}

export function categoryName(category: string): string {
  return LABELS[category] ?? category
}
