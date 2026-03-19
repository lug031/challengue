package com.challenge.inventory.dto;

import java.util.List;

public record AllocateInventoryRequest(String orderId, List<AllocateItemRequest> items) {
}

