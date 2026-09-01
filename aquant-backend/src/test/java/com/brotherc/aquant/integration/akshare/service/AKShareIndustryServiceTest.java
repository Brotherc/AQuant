package com.brotherc.aquant.integration.akshare.service;

import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AKShareIndustryServiceTest {

    @Test
    void extractsTheThsSectorCodeWhetherOrNotTheLinkHasATrailingSlash() {
        assertThat(AKShareIndustryService.parseThsSectorCode(
                "http://q.10jqka.com.cn/thshy/detail/code/881274/"))
                .isEqualTo("881274");
        assertThat(AKShareIndustryService.parseThsSectorCode(
                "http://q.10jqka.com.cn/thshy/detail/code/881274"))
                .isEqualTo("881274");
    }

    @Test
    void parsesThsIndustryConstituentRowsAndSkipsIncompleteRows() {
        String cells = "<td>1</td><td>600000</td><td>浦发银行</td><td>10.25</td>"
                + "<td>1.20</td><td>0.12</td><td>0</td><td>0</td><td>0</td>"
                + "<td>2.30</td><td>1.50亿</td><td>0</td><td>0</td><td>5.60</td>";
        String html = "<div id='maincont'><table><tbody>"
                + "<tr>" + cells + "</tr>"
                + "<tr><td>1</td><td>incomplete</td></tr>"
                + "</tbody></table></div>";

        List<StockBoardIndustryConsThs> result = AKShareIndustryService.parseThsConstituents(Jsoup.parse(html));

        assertThat(result).hasSize(1);
        StockBoardIndustryConsThs stock = result.get(0);
        assertThat(stock.getStockCode()).isEqualTo("600000");
        assertThat(stock.getStockName()).isEqualTo("浦发银行");
        assertThat(stock.getLatestPrice()).isEqualByComparingTo("10.25");
        assertThat(stock.getChangePercent()).isEqualByComparingTo("1.20");
        assertThat(stock.getChangeAmount()).isEqualByComparingTo("0.12");
        assertThat(stock.getAmplitude()).isEqualByComparingTo("2.30");
        assertThat(stock.getTurnover()).isEqualByComparingTo("150000000");
        assertThat(stock.getPeTtm()).isEqualByComparingTo("5.60");
    }

    @Test
    void returnsNoConstituentsForAnEmptyTable() {
        assertThat(AKShareIndustryService.parseThsConstituents(
                Jsoup.parse("<div id='maincont'><table><tbody></tbody></table></div>")))
                .isEmpty();
    }
}
