package com.argo.security;

import com.argo.common.ApiException;
import com.argo.common.ErrorCode;
import com.argo.common.Flag;
import com.argo.common.PageResult;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 運維查看異常 IP：把每天的紀錄加總後排序
@Service
public class IpAdminService {

	private static final int MAX_DAYS = 30;
	private static final int MAX_SIZE = 100;
	private static final Pattern PREFIX = Pattern.compile("^[0-9a-fA-F:.]{1,45}$");

	private final JdbcTemplate jdbc;
	private final IpBlockService blocks;

	public IpAdminService(JdbcTemplate jdbc, IpBlockService blocks) {
		this.jdbc = jdbc;
		this.blocks = blocks;
	}

	@Transactional(readOnly = true)
	public PageResult<IpActivityView> activity(int days, String prefix, int page, int size) {
		if (days < 1 || days > MAX_DAYS || page < 1 || size < 1 || size > MAX_SIZE) {
			throw new ApiException(ErrorCode.INVALID_PAGING);
		}
		String like = null;
		if (prefix != null && !prefix.isBlank()) {
			if (!PREFIX.matcher(prefix.trim()).matches()) {
				throw new ApiException(ErrorCode.BAD_REQUEST);
			}
			like = prefix.trim().toLowerCase() + "%";
		}
		Date since = Date.valueOf(LocalDate.now().minusDays(days - 1L));
		List<Object> args = new ArrayList<>(List.of(since));
		String where = "where day >= ?";
		if (like != null) {
			where += " and ip like ?";
			args.add(like);
		}
		long total = jdbc.queryForObject("select count(distinct ip) from ip_activity " + where, Long.class, args.toArray());
		args.add(size);
		args.add((page - 1) * size);
		Map<String, IpBlock> active = blocks.activeByIp();
		List<IpActivityView> items = jdbc.query("""
				select ip, sum(rate_limited) rl, sum(login_failed) lf, sum(blocked_hits) bh,
				       min(first_seen) fs, max(last_seen) ls
				from ip_activity %s
				group by ip
				order by sum(rate_limited) + sum(login_failed) + sum(blocked_hits) desc, max(last_seen) desc, ip
				limit ? offset ?
				""".formatted(where), (rs, i) -> {
			String ip = rs.getString("ip");
			IpBlock b = active.get(ip);
			return new IpActivityView(ip, rs.getLong("rl"), rs.getLong("lf"), rs.getLong("bh"),
					rs.getObject("fs", OffsetDateTime.class), rs.getObject("ls", OffsetDateTime.class),
					Flag.of(b != null), b == null ? null : b.getExpiresAt(), b == null ? null : b.getReason(),
					Flag.of(b != null && b.isAuto()));
		}, args.toArray());
		int pages = (int) Math.ceil(total / (double) size);
		return new PageResult<>(items, page, size, total, pages);
	}

	public List<IpBlockView> activeBlocks() {
		return blocks.listActive().stream().map(IpBlockView::from).toList();
	}
}
