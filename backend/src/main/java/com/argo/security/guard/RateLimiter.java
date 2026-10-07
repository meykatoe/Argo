package com.argo.security.guard;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 每個 IP、每種類別各自計算的固定時間窗限速，資料只存在記憶體
@Component
public class RateLimiter {

	public enum Kind {
		// 所有 API
		GENERAL,
		// 登入與註冊
		AUTH,
		// 下單、付款、取消
		CHECKOUT
	}

	public record Decision(boolean allowed, Kind kind, long retryAfterSeconds) {
	}

	private record Key(Kind kind, String ip) {
	}

	private record Window(long start, int count) {
	}

	private static final long WINDOW_MILLIS = 60_000;

	private final Map<Kind, Integer> limits = new EnumMap<>(Kind.class);
	private final LongSupplier clock;
	private final ConcurrentHashMap<Key, Window> windows = new ConcurrentHashMap<>();

	@org.springframework.beans.factory.annotation.Autowired
	public RateLimiter(@Value("${argo.ratelimit.general-per-minute:300}") int general,
			@Value("${argo.ratelimit.auth-per-minute:20}") int auth,
			@Value("${argo.ratelimit.checkout-per-minute:30}") int checkout) {
		this(general, auth, checkout, System::currentTimeMillis);
	}

	RateLimiter(int general, int auth, int checkout, LongSupplier clock) {
		limits.put(Kind.GENERAL, general);
		limits.put(Kind.AUTH, auth);
		limits.put(Kind.CHECKOUT, checkout);
		this.clock = clock;
	}

	// 依序計次，任何一類超過就拒絕
	public Decision check(String ip, List<Kind> kinds) {
		for (Kind k : kinds) {
			Decision d = hit(k, ip);
			if (!d.allowed()) {
				return d;
			}
		}
		return new Decision(true, null, 0);
	}

	private Decision hit(Kind kind, String ip) {
		long now = clock.getAsLong();
		Window w = windows.compute(new Key(kind, ip), (k, old) ->
				old == null || now - old.start() >= WINDOW_MILLIS
						? new Window(now, 1)
						: new Window(old.start(), old.count() + 1));
		if (w.count() <= limits.get(kind)) {
			return new Decision(true, kind, 0);
		}
		long wait = (w.start() + WINDOW_MILLIS - now + 999) / 1000;
		return new Decision(false, kind, Math.max(1, wait));
	}

	// 這次請求要算哪些類別
	public static List<Kind> kindsFor(String method, String path) {
		if (!"POST".equals(method)) {
			return List.of(Kind.GENERAL);
		}
		if (path.equals("/api/auth/login") || path.equals("/api/auth/register")
				|| path.equals("/api/admin/auth/login") || path.equals("/api/ops/auth/login")) {
			return List.of(Kind.GENERAL, Kind.AUTH);
		}
		if (path.equals("/api/orders") || path.matches("^/api/orders/[^/]+/(pay|cancel)$")) {
			return List.of(Kind.GENERAL, Kind.CHECKOUT);
		}
		return List.of(Kind.GENERAL);
	}

	// 清掉過期的時間窗，避免記憶體越長越大
	@Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
	public void purge() {
		long now = clock.getAsLong();
		windows.entrySet().removeIf(e -> now - e.getValue().start() >= 2 * WINDOW_MILLIS);
	}

	int size() {
		return windows.size();
	}
}
