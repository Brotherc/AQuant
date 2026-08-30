package com.brotherc.aquant.integration.htf.service;

import com.brotherc.aquant.integration.htf.model.HTFFundAnnouncement;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HTFFundServiceTest {

    private final HTFFundService service = new HTFFundService(null);

    @Test
    void shouldFindLatestNasdaq100LimitAnnouncement() {
        String html = """
                <table class="sharetable">
                  <tr><td><a href="/main/a/20260814/12866766.shtml">基金经理变更公告</a></td>
                    <td>2026-08-14</td><td><a class="yellowline" href="/other.pdf">下载</a></td></tr>
                  <tr><td><a href="/main/a/20260717/12865832.shtml?v=1">关于汇添富纳斯达克100交易型开放式
                    指数证券投资基金发起式联接基金（QDII）调整大额申购、定期定额投资业务限制金额的公告</a></td>
                    <td>2026-07-17</td><td><a class="yellowline"
                    href="/announcement/zx/upload/2026/latest.pdf">下载</a></td></tr>
                </table>
                """;

        HTFFundAnnouncement result = service.parseLatestAnnouncement(
                html.getBytes(Charset.forName("GB18030")),
                HttpUrl.get("https://www.99fund.com/main/products/pofund/018966/fundgg.shtml")
        );

        assertThat(result.getAnnouncementId()).isEqualTo("12865832");
        assertThat(result.getAnnouncementDate()).isEqualTo(LocalDate.of(2026, 7, 17));
        assertThat(result.getDetailUrl())
                .isEqualTo("https://www.99fund.com/main/a/20260717/12865832.shtml?v=1");
        assertThat(result.getAttachmentUrl())
                .isEqualTo("https://www.99fund.com/announcement/zx/upload/2026/latest.pdf");
    }

}
