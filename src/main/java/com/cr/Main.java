package com.cr;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import io.gate.gateapi.ApiClient;
import io.gate.gateapi.ApiException;
import io.gate.gateapi.Configuration;
import io.gate.gateapi.GateApiException;
import io.gate.gateapi.api.SpotApi;
import io.gate.gateapi.models.Currency;
import io.gate.gateapi.models.CurrencyPair;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import static com.cr.Utils.*;

@Slf4j
public class Main {

    public static void main(String[] args) {
        //handleSinglePair("UFO_USDT");
        handleAllPair();
    }

    public static void handleAllPair() {
        List<CurrencyPair> pairs = pairs();
        log.info("总计算数量: {}", pairs.size());
        for (int i = 0; i < pairs.size(); i++) {
            String pairId = pairs.get(i).getId();
            boolean ifFilter = filterCoin(pairId);
            if (ifFilter) {
                log.error("不需要计算 {}市场", pairId);
                continue;
            }
            List<List<String>> response = listCandlesticks(pairId);
            log.debug("远程请求返回数据量:{}", response.size());
            ForkJoinPool.commonPool().submit(new CalMarginFreq(pairId, response));
        }
    }

    public static void handleSinglePair(String pairId) {
        List<List<String>> response = listCandlesticks(pairId);
        ForkJoinPool.commonPool().submit(new CalMarginFreq(pairId, response));
    }

    public static List<Currency> currencies(SpotApi API) {
        try {
            return API.listCurrencies();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    public static List<CurrencyPair> pairs() {
        try {
            return spotAPI.listCurrencyPairs();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * [时间戳,交易量,收盘价,最高价,最低价,开盘价]
     * [1637020800, 3306152.010821043944, 163.63, 173.15, 158.29, 173.15]
     */
    public static List<List<String>> listCandlesticks(String pair) {
        try {
            List<List<String>> apiResult = spotAPI.listCandlesticks(pair).from(FROM).to(TO).interval(INTERNAL).execute();
            return apiResult;
        } catch (ApiException e) {
            log.error("Exception when calling SpotApi#listCandlesticks, code: {}, headers: {}", e.getCode(), e.getResponseHeaders());
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    static class CalMarginFreq extends RecursiveAction {
        //交易对
        private String pairId;
        //历史K线
        private List<List<String>> line;

        public CalMarginFreq(String pairId, List<List<String>> line) {
            this.pairId = pairId;
            this.line = line;
        }

        /**
         * 计算涨幅波动频率
         */
        @Override
        protected void compute() {
            //记录起始价格日期
            DateTime beginTime = null;
            //计算波动的起始价格
            BigDecimal begin = null;
            //波动频率次数
            int freg = 0;

            for (List<String> k : line) {
                DateTime date = DateUtil.date(Long.parseLong(k.get(0)) * 1000);
                BigDecimal todayOpen = Utils.scale(k.get(5));
                BigDecimal todayHigh = Utils.scale(k.get(3));
                BigDecimal todayLow = Utils.scale(k.get(4));
                BigDecimal todayClose = Utils.scale(k.get(2));
                log.debug("时间: {}, 开盘: {}, 最高: {}, 最低: {}, 闭盘: {}", date, stringOfBigDecimal(todayOpen), stringOfBigDecimal(todayHigh), stringOfBigDecimal(todayLow), stringOfBigDecimal(todayClose));

                if (Objects.isNull(begin)) { //以第一日闭盘价格为起始价格进行计算
                    begin = todayClose;
                    beginTime = date;
                    continue;
                }

                LineColor color = LineColor.color(todayOpen, todayClose);
                if (LineColor.GREEN.equals(color)) { //跌行情
                    if (todayLow.compareTo(begin) < 1) { //情况1和情况2
                        log.debug("{}市场跌行情需要重新调整起始点: {} > {}", pairId, stringOfBigDecimal(begin), stringOfBigDecimal(todayLow));
                        begin = todayLow;
                        beginTime = date;
                    } else { //情况3
                        log.debug("{}市场跌行情无需调整起始点, 日期{}", pairId, date);
                    }
                }
                if (LineColor.RED.equals(color)) { //涨行情
                    if (todayHigh.compareTo(begin) < 1) { //比如开盘跳水的情况
                        log.debug("{}市场涨行情需要重新调整起始点, 日期:{}", pairId, date);
                        begin = todayLow;
                        beginTime = date;
                    }

                    BigDecimal percentRange = Utils.calPercentRange(begin, todayHigh, todayLow); //涨幅百分比

                    if (percentRange.compareTo(MARGIN) > 0) { //超过自定义涨幅
                        freg++; //波动+1
                        log.warn("{}超过涨幅{}次, 涨幅: {}, 日期:{}-{}", pairId, freg, percentRange, beginTime, date);
                        begin = todayClose; //计算下一次拉盘
                        beginTime = date;
                    } else { //小趋势继续跟踪
                        log.debug("{}涨行情继续跟踪", pairId);
                        if (todayLow.compareTo(begin) < 1) { //情况1
                            begin = todayLow;
                            beginTime = date;
                        }
                    }
                }
            }

            if (freg >= FREQ) { //超过N次记录
                log.info("庄狗币 - [{}], 拉盘 - [{}]", pairId ,freg);
            }
        }
    }

    //哪天开始
    static long FROM = DateUtil.lastMonth().getTime() / 1000;
    //哪天结束
    static long TO = DateUtil.currentSeconds();
    //一天的交易
    static String INTERNAL = "1d";
    //幅度
    static BigDecimal MARGIN = new BigDecimal("0.3");
    //频率
    static int FREQ = 5;

    static ApiClient client;
    static SpotApi spotAPI;

    static {
        client = Configuration.getDefaultApiClient();
        spotAPI = new SpotApi(client);
    }

}
