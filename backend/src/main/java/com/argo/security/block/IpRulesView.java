package com.argo.security.block;

import java.util.List;

public record IpRulesView(List<AutoBlockRuleView> rules, List<AllowEntryView> allowlist) {
}
