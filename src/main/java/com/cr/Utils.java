package com.cr;

import cn.hutool.core.io.file.FileWriter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class Utils {

    /**
     * 写入文件
     */
    public static void write(String content) {
        FileWriter file = new FileWriter(System.getProperty("user.dir") + "/record");
        file.append(content);
    }

    /**
     * 设置精度
     */
    public static BigDecimal scale(String num){
        return new BigDecimal(num).setScale(10, RoundingMode.HALF_UP);
    }

    /**
     * 打印BigDecimal
     */
    public static String stringOfBigDecimal(BigDecimal bd){
        return bd.stripTrailingZeros().toPlainString();
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
    public static BigDecimal calTRByBegin(BigDecimal begin, BigDecimal high, BigDecimal low) {
        BigDecimal max = high.compareTo(begin) == 1 ? high : begin;
        BigDecimal min = low.compareTo(begin) == -1 ? low : begin;
        return max.subtract(min);
    }


}
