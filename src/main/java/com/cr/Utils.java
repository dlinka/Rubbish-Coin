package com.cr;

import cn.hutool.core.io.file.FileWriter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class Utils {

    /**
     * write
     */
    public static void write(String content) {
        FileWriter file = new FileWriter("~/record");
        file.write(content);
    }

    /**
     * 精度 0.00
     */
    public static BigDecimal scale(String num){
        return new BigDecimal(num).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 真实波动幅度
     */
    public static BigDecimal calTRByClose(BigDecimal close, BigDecimal high, BigDecimal low){
        BigDecimal max = high.compareTo(close) == 1 ? high : close;
        BigDecimal min = low.compareTo(close) == -1 ? low : close;
        return max.subtract(min);
    }

    /**
     * 真实波动幅度
     */
    public static BigDecimal calTRByOpen(BigDecimal open, BigDecimal high, BigDecimal low) {
        BigDecimal max = high.compareTo(open) == 1 ? high : open;
        BigDecimal min = low.compareTo(open) == -1 ? low : open;
        return max.subtract(min);
    }


}
