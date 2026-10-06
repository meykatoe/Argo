package com.argo.security;

import java.util.List;

public record IpRulesView(List<AutoBlockRuleView> rules, List<AllowEntryView> allowlist) {
}
