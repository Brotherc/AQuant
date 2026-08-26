package com.brotherc.aquant.integration.th.service;

import com.brotherc.aquant.integration.th.model.THFundAnnouncementPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class THFundServiceTest {

    @Test
    void shouldParseDisclosureAnnouncementPageAndBuildPdfUrl() throws Exception {
        THFundService service = new THFundService(null, new ObjectMapper());
        ReflectionTestUtils.setField(service, "thFundAddress", "https://www.thfund.com.cn");
        ReflectionTestUtils.setField(service, "thFundDisclosureCdnAddress", "https://pdf.dfcfw.com");
        String json = """
                {
                  "Data": [{
                    "FUNDCODE": "018043",
                    "TITLE": "天弘基金关于天弘纳斯达克100指数基金暂停申购的公告",
                    "PUBLISHDATEDesc": "2026-05-29",
                    "ID": "AN202605291822989315"
                  }],
                  "ErrCode": 0,
                  "TotalCount": 21,
                  "PageSize": 20,
                  "PageIndex": 1
                }
                """;

        THFundAnnouncementPage page = service.parseAnnouncementPage(json.getBytes(StandardCharsets.UTF_8));

        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).singleElement().satisfies(announcement -> {
            assertThat(announcement.getAnnouncementId()).isEqualTo("AN202605291822989315");
            assertThat(announcement.getAnnouncementDate()).hasToString("2026-05-29");
            assertThat(announcement.getTitle()).contains("暂停申购");
            assertThat(announcement.getDetailUrl()).startsWith("https://www.thfund.com.cn/notice_list");
            assertThat(announcement.getAttachmentUrl())
                    .isEqualTo("https://pdf.dfcfw.com/pdf/H2_AN202605291822989315_1.pdf");
        });
    }

}
