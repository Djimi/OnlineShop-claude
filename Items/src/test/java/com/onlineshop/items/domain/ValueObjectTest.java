package com.onlineshop.items.domain;

import com.onlineshop.items.domain.valueobject.ItemDescription;
import com.onlineshop.items.domain.valueobject.ItemName;
import com.onlineshop.items.domain.valueobject.Quantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectTest {

    @Test
    void itemName_shouldRejectNull() {
        assertThatThrownBy(() -> new ItemName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item name cannot be blank");
    }

    @Test
    void itemName_shouldRejectBlank() {
        assertThatThrownBy(() -> new ItemName("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item name cannot be blank");
    }

    @Test
    void itemName_shouldRejectOverMaxLength() {
        String twoHundredOneChars = "a".repeat(201);
        assertThatThrownBy(() -> new ItemName(twoHundredOneChars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item name too long");
    }

    @Test
    void itemName_shouldAcceptValidName() {
        ItemName name = new ItemName("Valid Item Name");
        assertThat(name.value()).isEqualTo("Valid Item Name");
    }

    @Test
    void itemName_shouldAcceptMaxLength() {
        String twoHundredChars = "a".repeat(200);
        ItemName name = new ItemName(twoHundredChars);
        assertThat(name.value()).hasSize(200);
    }

    @Test
    void itemDescription_shouldConvertNullToEmpty() {
        ItemDescription desc = new ItemDescription(null);
        assertThat(desc.value()).isEmpty();
    }

    @Test
    void itemDescription_shouldRejectOverMaxLength() {
        String fiveHundredOneChars = "a".repeat(501);
        assertThatThrownBy(() -> new ItemDescription(fiveHundredOneChars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item description too long");
    }

    @Test
    void itemDescription_shouldAcceptValidDescription() {
        ItemDescription desc = new ItemDescription("A valid description");
        assertThat(desc.value()).isEqualTo("A valid description");
    }

    @Test
    void itemDescription_shouldAcceptMaxLength() {
        String fiveHundredChars = "a".repeat(500);
        ItemDescription desc = new ItemDescription(fiveHundredChars);
        assertThat(desc.value()).hasSize(500);
    }

    @Test
    void quantity_shouldRejectNegative() {
        assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity cannot be negative");
    }

    @Test
    void quantity_shouldAcceptZero() {
        Quantity qty = new Quantity(0);
        assertThat(qty.amount()).isZero();
        assertThat(qty.isGreaterThanZero()).isFalse();
    }

    @Test
    void quantity_shouldAcceptPositive() {
        Quantity qty = new Quantity(5);
        assertThat(qty.amount()).isEqualTo(5);
        assertThat(qty.isGreaterThanZero()).isTrue();
    }

    @Test
    void quantity_equals_shouldCompareByValue() {
        assertThat(new Quantity(5)).isEqualTo(new Quantity(5));
        assertThat(new Quantity(5)).isNotEqualTo(new Quantity(10));
    }
}
