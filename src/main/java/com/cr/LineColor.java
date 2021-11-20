package com.cr;

import java.math.BigDecimal;

public enum LineColor {
    RED, GREEN;

    public static LineColor color(BigDecimal open, BigDecimal close){
        return close.compareTo(open) > 0 ? LineColor.RED : LineColor.GREEN;
    }
}
