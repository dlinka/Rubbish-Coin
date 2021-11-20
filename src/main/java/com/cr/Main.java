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

@Slf4j
public class Main {

    //哪天开始
    static long FROM = DateUtil.lastWeek().getTime() / 1000;
    //哪天结束
    static long TO = DateUtil.currentSeconds();
    //只计算一天的交易
    static String INTERNAL = "1d";
    //幅度
    static BigDecimal MARGIN = new BigDecimal("0.01");
    //频率
    static int FREQ = 3;

    public static void main(String[] args) throws ApiException {
        ApiClient client = Configuration.getDefaultApiClient();
        SpotApi spotAPI = new SpotApi(client);


        List<CurrencyPair> pairs = pairs(spotAPI);
        pairs.forEach(p -> {
            log.debug("{}", p.getId());
        });

        for (CurrencyPair pair : pairs){
            calMarginFreq(spotAPI, pair.getId(), FROM, TO);
        }
    }

    public static List<Currency> currencies(SpotApi API) {
        try {
            return API.listCurrencies();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    public static List<CurrencyPair> pairs(SpotApi API) {
        try {
            return API.listCurrencyPairs();
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * 计算涨幅波动频率
     * listCandlesticks response
     * [时间戳,交易量,收盘价,最高价,最低价,开盘价]
     * [1637020800, 3306152.010821043944, 163.63, 173.15, 158.29, 173.15]
     */
    public static void calMarginFreq(SpotApi API, String pair, Long from, Long to) {
        try {
            List<List<String>> apiResult = API.listCandlesticks(pair).from(from).to(to).interval(INTERNAL).execute();

            //计算波动的起始价格
            BigDecimal begin = null;
            //波动频率次数
            int freg = 0;

            for (List<String> k : apiResult) {
                DateTime date = DateUtil.date(Long.parseLong(k.get(0)) * 1000);
                BigDecimal todayOpen = Utils.scale(k.get(5));
                BigDecimal todayHigh = Utils.scale(k.get(3));
                BigDecimal todayLow = Utils.scale(k.get(4));
                BigDecimal todayClose = Utils.scale(k.get(2));
                log.debug("时间: {}, 开盘: {}, 最高: {}, 最低: {}, 闭盘: {}", date, todayOpen, todayHigh, todayLow, todayClose);

                if (Objects.isNull(begin)) { //拿到第一日最低价格
                    begin = todayLow;
                    continue;
                }

                if (LineColor.color(todayOpen, todayClose).equals(LineColor.RED)) { //涨行情
                    BigDecimal trueRange = Utils.calTRByOpen(begin, todayHigh, todayLow);
                    BigDecimal percentRange = trueRange.divide(begin, 2, RoundingMode.HALF_UP);
                    if (percentRange.compareTo(MARGIN) > 0) { //超过自定义涨幅
                        freg++; //波动+1
                        log.warn("{}超过涨幅{}次, 涨幅: {}", pair, freg, percentRange);
                        if (freg == FREQ) { //超过3次记录
                            Utils.write(pair + "\n");
                        }
                        begin = todayClose; //计算下一次拉盘
                    } else {
                        log.warn("涨行情继续跟踪");
                        begin = begin.compareTo(todayLow) > 0 ? todayLow : begin; //小趋势继续跟踪
                    }
                } else { //跌行情
                    if (begin.compareTo(todayLow) > 0) {
                        log.info("跌行情重新计算起始点: {} > {}", begin, todayLow);
                        begin = todayLow;
                    }
                }
            }
        } catch (GateApiException e) {
            log.error("Gate api exception, label: {}, message: {}", e.getErrorLabel(), e.getMessage());
            e.printStackTrace();
        } catch (ApiException e) {
            log.error("Exception when calling SpotApi#listCandlesticks, code: {}, headers: {}", e.getCode(), e.getResponseHeaders());
            e.printStackTrace();
        }
    }


}
