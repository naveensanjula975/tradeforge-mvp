package com.tradeforge.order.domain.vo;

import com.tradeforge.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Price value object")
class PriceTest {

    @Test
    @DisplayName("create valid positive price")
    void create_validPrice_succeeds() {
        Price price = Price.of(new BigDecimal("100.50"));
        assertThat(price.getValue()).isEqualByComparingTo("100.50");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.001"})
    @DisplayName("reject zero or negative price")
    void create_zeroOrNegativePrice_throws(String rawPrice) {
        assertThatThrownBy(() -> Price.of(new BigDecimal(rawPrice)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("null price throws NullPointerException")
    void create_nullPrice_throws() {
        assertThatNullPointerException().isThrownBy(() -> Price.of((BigDecimal) null));
    }

    @Test
    @DisplayName("price on tick passes validation")
    void requireOnTick_alignedPrice_passes() {
        Price price = Price.of(new BigDecimal("100.0"));
        assertThatCode(() -> price.requireOnTick(new BigDecimal("0.10"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("price not on tick throws BusinessRuleException")
    void requireOnTick_misalignedPrice_throws() {
        Price price = Price.of(new BigDecimal("100.05"));
        assertThatThrownBy(() -> price.requireOnTick(new BigDecimal("0.10")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("price equality ignores trailing zeros")
    void equals_differentScale_areEqual() {
        Price p1 = Price.of(new BigDecimal("100.50"));
        Price p2 = Price.of(new BigDecimal("100.5"));
        assertThat(p1).isEqualTo(p2);
    }

    @Test
    @DisplayName("compareTo works correctly")
    void compareTo_differentPrices_correctOrder() {
        Price low  = Price.of(new BigDecimal("80"));
        Price high = Price.of(new BigDecimal("100"));
        assertThat(low).isLessThan(high);
        assertThat(high).isGreaterThan(low);
    }
}
