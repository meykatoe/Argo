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
