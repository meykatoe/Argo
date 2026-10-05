import { get } from './http'
import type {
  CardCategory,
  CardDetail,
  CardSearchParams,
  CardSet,
  CardSummary,
  PageResult,
} from '@/types/card'

export function searchCards(params: CardSearchParams = {}) {
  return get<PageResult<CardSummary>>('/cards', { ...params })
}

export function getCard(id: number) {
  return get<CardDetail>(`/cards/${id}`)
}

export function listSets(category?: CardCategory) {
  return get<CardSet[]>('/sets', { category })
}

const BATCH_SIZE = 50

// 後端一次最多五十筆，超過就分批
export async function getCardsByIds(ids: number[]): Promise<CardSummary[]> {
  const chunks: number[][] = []
  for (let i = 0; i < ids.length; i += BATCH_SIZE) {
    chunks.push(ids.slice(i, i + BATCH_SIZE))
  }
  const results = await Promise.all(
    chunks.map((c) => get<CardSummary[]>('/cards/batch', { ids: c.join(',') })),
  )
  return results.flat()
}
