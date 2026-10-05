package com.argo.card;

import com.argo.common.PageResult;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CardService {

	// 排序欄位白名單
	private static final Set<String> SORTS = Set.of("cardSetId", "cardName", "marketPrice");
	private static final int MAX_SIZE = 100;

	private final CardRepository cards;
	private final CardSetRepository sets;

	public CardService(CardRepository cards, CardSetRepository sets) {
		this.cards = cards;
		this.sets = sets;
	}

	public PageResult<CardSummary> search(CardQuery query, int page, int size,
			String sortBy, boolean desc) {
		// 頁碼從一起算
		if (page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分頁參數不正確");
		}
		if (!SORTS.contains(sortBy)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支援的排序欄位");
		}
		// 加主鍵穩定分頁
		Sort sort = Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy)
				.and(Sort.by("id"));
		var result = cards.findAll(CardSpecs.of(query), PageRequest.of(page - 1, size, sort));
		return PageResult.of(result, CardSummary::from);
	}

	public CardDetail get(Long id) {
		Card card = cards.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到卡片"));
		String setName = sets.findById(card.getSetId()).map(CardSet::getSetName).orElse(null);
		return CardDetail.from(card, setName);
	}

	public List<CardSetDto> listSets(String category) {
		List<CardSet> all = category == null || category.isBlank()
				? sets.findAll(Sort.by("setId"))
				: sets.findByCategory(category, Sort.by("setId"));
		return all.stream().map(CardSetDto::from).toList();
	}
}
