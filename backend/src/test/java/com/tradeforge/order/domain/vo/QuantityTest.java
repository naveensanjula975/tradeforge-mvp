package com.tradeforge.order.domain.vo;

import com.tradeforge.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Quantity value object")
class QuantityTest {

    @Test
    @DisplayName("create valid positive quantity")
    void create_validQuantity_succeeds() {
        Quantity q = Quantity.of(new BigDecimal("100"));
        assertThat(q.getValue()).isEqualByComparingTo("100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10"})
    @DisplayName("reject zero or negative quantity")
    void create_zeroOrNegative_throws(String raw) {
        assertThatThrownBy(() -> Quantity.of(new BigDecimal(raw)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("quantity on lot passes")
    void requireOnLot_aligned_passes() {
        Quantity q = Quantity.of(new BigDecimal("100"));
        assertThatCode(() -> q.requireOnLot(new BigDecimal("10"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("quantity not on lot throws")
    void requireOnLot_misaligned_throws() {
        Quantity q = Quantity.of(new BigDecimal("15"));
        assertThatThrownBy(() -> q.requireOnLot(new BigDecimal("10")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("subtract within bounds succeeds")
    void subtract_withinBounds_returnsCorrectRemainder() {
        Quantity q100 = Quantity.of(new BigDecimal("100"));
        Quantity q40  = Quantity.of(new BigDecimal("40"));
        Quantity result = q100.subtract(q40);
        assertThat(result.getValue()).isEqualByComparingTo("60");
    }

    @Test
    @DisplayName("subtract beyond quantity throws")
    void subtract_beyondQuantity_throws() {
        Quantity q10  = Quantity.of(new BigDecimal("10"));
        Quantity q100 = Quantity.of(new BigDecimal("100"));
        assertThatThrownBy(() -> q10.subtract(q100))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("equality ignores trailing zeros")
    void equals_differentScale_areEqual() {
        Quantity q1 = Quantity.of(new BigDecimal("100.0"));
        Quantity q2 = Quantity.of(new BigDecimal("100"));
        assertThat(q1).isEqualTo(q2);
    }
}
