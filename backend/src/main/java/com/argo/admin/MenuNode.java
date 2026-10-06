package com.argo.admin;

import java.util.List;

public record MenuNode(String code, String title, String path, List<MenuNode> children) {
}
