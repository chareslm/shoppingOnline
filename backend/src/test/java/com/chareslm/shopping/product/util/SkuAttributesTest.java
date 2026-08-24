package com.chareslm.shopping.product.util;

import com.chareslm.shopping.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkuAttributesTest {

    @Test
    void blankBecomesNull() {
        assertNull(SkuAttributes.normalize(null));
        assertNull(SkuAttributes.normalize("  "));
    }

    @Test
    void keepsJsonObject() {
        assertEquals("{\"颜色\":\"黑\"}", SkuAttributes.normalize("{\"颜色\":\"黑\"}"));
    }

    @Test
    void parsesColonPairs() {
        assertEquals("{\"颜色\":\"黑\",\"内存\":\"256GB\"}", SkuAttributes.normalize("颜色:黑,内存:256GB"));
    }

    @Test
    void wrapsPlainText() {
        assertEquals("{\"规格\":\"黑色\"}", SkuAttributes.normalize("黑色"));
    }

    @Test
    void rejectsBrokenJsonObject() {
        assertThrows(BusinessException.class, () -> SkuAttributes.normalize("{not-json"));
    }
}
