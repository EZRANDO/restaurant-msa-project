package com.example.order.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class OrderRequest {
    private List<OrderItemDto> items;
    private String couponCode;

    @Getter
    public static class OrderItemDto {
        private Long menuId;
        private String menuName;
        private Integer quantity;
        private Integer price;
    }
}
