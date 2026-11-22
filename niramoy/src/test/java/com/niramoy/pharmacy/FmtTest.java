package com.niramoy.pharmacy;

import com.niramoy.pharmacy.controller.Fmt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FmtTest {
    private final Fmt fmt = new Fmt();

    @Test
    void formatsMoneyWithGrouping() {
        assertEquals("৳1,234.50", fmt.money(new BigDecimal("1234.5")));
    }

    @Test
    void nullMoneyIsZero() {
        assertEquals("৳0.00", fmt.money(null));
    }
}
