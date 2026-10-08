package com.argo.order.admin;

import jakarta.validation.constraints.Size;

public record NoteRequest(@Size(max = 500) String note) {
}
