package com.challenge.inventory.dto;

import java.util.List;

public record AllocateInventoryResponse(String orderId, List<AllocatedItemResponse> allocatedItems) {
}

