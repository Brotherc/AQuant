package com.brotherc.aquant.integration.cmf.service;

import com.brotherc.aquant.integration.cmf.model.CMFFundPurchaseLimit;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CMFFundServiceTest {

    private final CMFFundService service = new CMFFundService(null);

    @Test
    void shouldFindLatestDirectLimitAnnouncement() {
        String html = """
                <ul class="pro_articlelist">
                  <li><a class="item" href="/web/noticedetails/225627/index.html">
                    <p>招商基金管理有限公司旗下基金中期报告</p><span class="date">2026-08-31</span></a></li>
                  <li><a class="item" href="/web/noticedetails/225156/index.html">
                    <p>关于调整招商纳斯达克100交易型开放式指数证券投资基金发起式联接基金（QDII）
                    在直销机构大额申购（含定期定额投资）业务的公告</p>
                    <span class="date">2026-07-24</span></a></li>
                  <li><a class="item" href="/web/noticedetails/224338/index.html">
                    <p>关于调整招商纳斯达克100基金大额申购业务的公告</p>
                    <span class="date">2026-06-09</span></a></li>
                </ul>
                """;

        CMFFundPurchaseLimit result = service.parseLatestAnnouncement(
                html.getBytes(StandardCharsets.UTF_8),
                HttpUrl.get("https://www.cmfchina.com/web/fundDetail/019547/index.html")
        );

        assertThat(result.getAnnouncementId()).isEqualTo("225156");
        assertThat(result.getAnnouncementDate()).isEqualTo(LocalDate.of(2026, 7, 24));
        assertThat(result.getDetailUrl())
                .isEqualTo("https://www.cmfchina.com/web/noticedetails/225156/index.html");
    }

    @Test
    void shouldParsePurchaseAndRecurringLimits() {
        CMFFundPurchaseLimit announcement = new CMFFundPurchaseLimit();
        announcement.setAnnouncementId("225156");
        announcement.setAnnouncementDate(LocalDate.of(2026, 7, 24));
        announcement.setTitle("关于调整招商纳斯达克100基金在直销机构大额申购（含定期定额投资）业务的公告");
        announcement.setDetailUrl("https://www.cmfchina.com/web/noticedetails/225156/index.html");
        String html = """
                <div class="article_detail_box">
                  <p>基金主代码 019547</p>
                  <p>下属分级基金的交易代码 019547 019548</p>
                  <p>暂停大额申购起始日 2026年7月27日</p>
                  <p>限制申购金额（单位：人民币元）100.00</p>
                  <p>注：暂停大额定期定额投资起始日：2026年7月27日</p>
                  <p>限制定期定额投资金额：100.00元</p>
                  <p>本基金在本公司直销机构大额申购业务。</p>
                </div>
                """;

        CMFFundPurchaseLimit result = service.parsePurchaseLimit(
                html.getBytes(StandardCharsets.UTF_8), announcement
        );

        assertThat(result.getPurchaseStatus()).isEqualTo("LIMITED");
        assertThat(result.getPurchaseLimitAmount()).isEqualByComparingTo("100");
        assertThat(result.getRecurringStatus()).isEqualTo("LIMITED");
        assertThat(result.getRecurringLimitAmount()).isEqualByComparingTo("100");
        assertThat(result.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 7, 27));
    }

}
