package com.bookstore.dto;

import com.bookstore.entity.Address;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private List<OrderItemRequest> items;
    private Address address;

    @Data
    public static class OrderItemRequest {
        private Long bookId;
        private Integer quantity;
    }
}
