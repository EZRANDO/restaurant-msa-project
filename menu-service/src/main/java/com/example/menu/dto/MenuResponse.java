package com.example.menu.dto;

import com.example.menu.domain.Menu;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MenuResponse {
    private Long id;
    private String name;
    private String description;
    private Integer price;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private boolean available;
    private LocalDateTime createdAt;

    public static MenuResponse from(Menu m) {
        MenuResponse r = new MenuResponse();
        r.id = m.getId();
        r.name = m.getName();
        r.description = m.getDescription();
        r.price = m.getPrice();
        r.imageUrl = m.getImageUrl();
        r.categoryId = m.getCategory() != null ? m.getCategory().getId() : null;
        r.categoryName = m.getCategory() != null ? m.getCategory().getName() : null;
        r.available = m.isAvailable();
        r.createdAt = m.getCreatedAt();
        return r;
    }
}
