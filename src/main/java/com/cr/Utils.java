package com.cr;

import cn.hutool.core.io.file.FileWriter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class Utils {

    public static void main(String[] args) {
        System.out.println(filterCoin("C983L_USDT"));
        System.out.println(filterCoin("HNS_BTC"));
        System.out.println(filterCoin("XTZ3L_USDT"));
        System.out.println(filterCoin("BENQI_USDT"));
        System.out.println(filterCoin("IOST3S_USDT"));
    }

    /**
     * 写入文件
     */
    public static void write(String content) {
        FileWriter file = new FileWriter(System.getProperty("user.dir") + "/record_" + LocalDateTime.now());
        file.append(content);
    }

    /**
     * 设置精度
     */
    public static BigDecimal scale(String num) {
        return new BigDecimal(num).setScale(10, RoundingMode.HALF_UP);
    }

    /**
     * 打印BigDecimal
     */
    public static String stringOfBigDecimal(BigDecimal bd) {
        return bd.stripTrailingZeros().toPlainString();
    }

    /**
     * 真实波动幅度
     */
    public static BigDecimal calTRByClose(BigDecimal close, BigDecimal high, BigDecimal low) {
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

    /**
     * 计算涨幅
     */
    public static BigDecimal calPercentRange(BigDecimal begin, BigDecimal todayHigh, BigDecimal todayLow) {
        BigDecimal trueRange = calTRByBegin(begin, todayHigh, todayLow); //真实波动
        return trueRange.divide(begin, 2, RoundingMode.HALF_UP);
    }

    /**
     * 过滤一些币
     * 不统计倍数市场,比如BCH5L_USDT(做多)、OMG3S_USDT(做空)等
     * 不统计以太币市场,比如FAR_ETH
     * 不统计比特币市场,比如LEO_BTC
     * ,只统计USDT市场
     */
    public static boolean filterCoin(String pairId) {
        return Pattern.matches(".*\\d(L|S)_USDT", pairId) ? true
                : Pattern.matches(".*_ETH", pairId) ? true
                : Pattern.matches(".*_BTC", pairId) ? true
                : false;
    }

}
