package com.argo.card;

import com.argo.common.PageResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") 
public class CardController {

	private final CardService service;

	public CardController(CardService service) {
		this.service = service;
	}

	// 列表
	@GetMapping("/cards") 
	public PageResult<CardSummary> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String setId,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String color,
			@RequestParam(required = false) String rarity,
			@RequestParam(required = false) String cardType,
			@RequestParam(defaultValue = "false") boolean inStock,
			@RequestParam(defaultValue = "en") String lang,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "cardSetId") String sortBy,
			@RequestParam(defaultValue = "false") boolean desc) {
		CardQuery query = new CardQuery(keyword, setId, category, color, rarity, cardType, inStock, lang);
		return service.search(query, page, size, sortBy, desc);
	}

	// 卡片詳情
	@GetMapping("/cards/{id}")
	public CardDetail detail(@PathVariable Long id,
			@RequestParam(defaultValue = "en") String lang) {
		return service.get(id, lang);
	}

	// 系列列表
	@GetMapping("/sets")
	public List<CardSetDto> sets(@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "en") String lang) {
		return service.listSets(category, lang);
	}
}
