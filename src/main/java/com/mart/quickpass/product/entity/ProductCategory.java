package com.mart.quickpass.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {
    DAIRY("유제품"),
    BEVERAGE("음료"),
    SNACK("과자"),
    FROZEN("냉동식품"),
    FRUIT("과일"),
    VEGETABLE("채소"),
    HOUSEHOLD("생활용품");

    private final String displayName;
}
