package com.terralite.game.item.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.game.item.Item;

public record ItemDeffinition(
        @JsonProperty(defaultValue = "1.0")
        float weight
        ) {
    public Item toItem() {
        return Item.builder()
                .weight(weight)
                .build();
    }
    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
