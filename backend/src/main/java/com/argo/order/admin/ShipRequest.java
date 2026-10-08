package com.argo.order.admin;

import jakarta.validation.constraints.Size;

public record ShipRequest(@Size(max = 50) String trackingNo) {
}
