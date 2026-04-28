package com.example.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class MenuRequest {
    @NotBlank private String name;
    private String description;
    @NotNull @Positive private Integer price;
    private String imageUrl;
    @NotNull private Long categoryId;
    private Boolean available;
}
