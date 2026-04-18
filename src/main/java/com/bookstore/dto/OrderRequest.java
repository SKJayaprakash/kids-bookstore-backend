package com.bookstore.dto;

import com.bookstore.entity.Address;
import java.util.List;

public class OrderRequest {
    private List<OrderItemRequest> items;
    private Address address;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public static class OrderItemRequest {
        private Long bookId;
        private Integer quantity;

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
