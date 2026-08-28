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
    HOUSEHOLD("생활용품"),
    FOOD("식품"),
    FASHION_ACCESSORIES("패션잡화"),
    DIGITAL_ELECTRONICS("디지털/가전"),
    TOYS_HOBBIES("완구/취미"),
    KITCHENWARE("주방용품"),
    SPORTS_LEISURE("스포츠/레저"),
    BEAUTY("화장품/미용");

    private final String displayName;
}
