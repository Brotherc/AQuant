package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EastmoneyJywgClientTest {

    @Mock
    private StockSyncRepository stockSyncRepository;

    private EastmoneyJywgClient client(String cookie, String validateKey) {
        return new EastmoneyJywgClient(new OkHttpClient(), new ObjectMapper(),
                "https://jywg.eastmoneysec.com", cookie, validateKey, stockSyncRepository);
    }

    @Test
    void startsUnconfiguredWithoutCookie() {
        EastmoneyJywgClient client = client("", "");
        assertThat(client.isConfigured()).isFalse();
        assertThat(client.sessionStatus())
                .containsEntry("configured", false)
                .containsEntry("expired", false);
    }

    @Test
    void configuredFromPropertiesIsUsableWithoutCountdown() {
        // 不可达地址：验证未计时的配置态会话会直接发起查询（过期由上游接口报错兜底）
        EastmoneyJywgClient client = new EastmoneyJywgClient(new OkHttpClient(), new ObjectMapper(),
                "https://127.0.0.1:1", "SESSION=abc", "vk123", stockSyncRepository);

        assertThat(client.isConfigured()).isTrue();
        Map<String, Object> status = client.sessionStatus();
        assertThat(status)
                .containsEntry("configured", true)
                .containsEntry("expired", false)
                .containsEntry("remainingSeconds", 0L)
                .containsEntry("expireAt", null);
        assertThatThrownBy(() -> client.todayMatches())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可达");
    }

    @Test
    void hotUpdatedSessionCountsDownPersistsUpdateTimeAndExpires() {
        EastmoneyJywgClient client = client("", "");
        long before = System.currentTimeMillis();
        client.updateSession("SESSION=abc", "vk123", 15);

        assertThat(client.isConfigured()).isTrue();
        Map<String, Object> status = client.sessionStatus();
        assertThat(status)
                .containsEntry("configured", true)
                .containsEntry("expired", false)
                .containsEntry("durationMinutes", 15);
        // 剩余秒数约 900（15 分钟）
        assertThat(((Number) status.get("remainingSeconds")).intValue()).isBetween(895, 900);
        // 记录更新时间（前端展示 + 重启恢复倒计时的依据）
        assertThat((CharSequence) status.get("updateTime")).isNotNull();

        // 更新时间与凭证持久化到 stock_sync，重启后可恢复倒计时
        ArgumentCaptor<StockSync> captor = ArgumentCaptor.forClass(StockSync.class);
        verify(stockSyncRepository).save(captor.capture());
        StockSync stored = captor.getValue();
        assertThat(stored.getName()).isEqualTo(EastmoneyJywgClient.SESSION_STORAGE_KEY);
        assertThat(stored.getValue())
                .contains("SESSION=abc").contains("vk123")
                .contains("\"durationMinutes\":15");
        long persistedUpdateTime = com.jayway.jsonpath.JsonPath.parse(stored.getValue())
                .read("$.updateTime", Long.class);
        assertThat(persistedUpdateTime).isBetween(before, System.currentTimeMillis());

        // 负时长模拟已到期的会话：查询直接拒绝并提示重新获取（上游 Cookie 同步过期），不再发起 HTTP 请求
        client.updateSession("SESSION=abc", "vk123", -1);
        assertThat(client.sessionStatus()).containsEntry("expired", true);
        assertThat(client.isConfigured()).isTrue();
        assertThatThrownBy(() -> client.todayMatches())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("交易会话已过期");
    }

    @Test
    void restoresPersistedSessionOnStartupWithContinuedCountdown() throws Exception {
        long updateTime = System.currentTimeMillis() - 5 * 60_000L; // 5 分钟前更新
        StockSync stored = new StockSync();
        stored.setName(EastmoneyJywgClient.SESSION_STORAGE_KEY);
        stored.setValue(new ObjectMapper().writeValueAsString(Map.of(
                "cookie", "SESSION=abc", "validateKey", "vk123",
                "durationMinutes", 30, "updateTime", updateTime)));
        when(stockSyncRepository.findByName(EastmoneyJywgClient.SESSION_STORAGE_KEY)).thenReturn(stored);

        EastmoneyJywgClient client = client("", "");
        assertThat(client.isConfigured()).isTrue();
        Map<String, Object> status = client.sessionStatus();
        // 30 分钟在线、已过 5 分钟 → 剩余约 25 分钟（倒计时按持久化的更新时间续算）
        assertThat(status).containsEntry("configured", true).containsEntry("expired", false);
        assertThat(((Number) status.get("remainingSeconds")).intValue()).isBetween(1490, 1510);
        assertThat((CharSequence) status.get("updateTime")).isNotNull();
    }

    @Test
    void restoredPersistedSessionAlreadyExpiredIsRejected() throws Exception {
        long updateTime = System.currentTimeMillis() - 31 * 60_000L; // 31 分钟前更新，30 分钟时长已到期
        StockSync stored = new StockSync();
        stored.setName(EastmoneyJywgClient.SESSION_STORAGE_KEY);
        stored.setValue(new ObjectMapper().writeValueAsString(Map.of(
                "cookie", "SESSION=abc", "validateKey", "vk123",
                "durationMinutes", 30, "updateTime", updateTime)));
        when(stockSyncRepository.findByName(EastmoneyJywgClient.SESSION_STORAGE_KEY)).thenReturn(stored);

        EastmoneyJywgClient client = client("", "");
        assertThat(client.sessionStatus()).containsEntry("expired", true);
        assertThatThrownBy(() -> client.todayMatches())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("交易会话已过期");
    }

    @Test
    void rejectsBlankSessionValues() {
        EastmoneyJywgClient client = client("", "");
        // 在线时长取值范围由控制器校验；客户端只校验凭证非空
        assertThatThrownBy(() -> client.updateSession(" ", "vk", 15))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
        assertThatThrownBy(() -> client.updateSession("c", " ", 30))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
        assertThat(client.isConfigured()).isFalse();
    }

    @Test
    void loginPageHtmlResponseIsReportedAsSessionExpired() throws Exception {
        // 会话失效时 jywg 返回 200 + 登录页 HTML：必须明确报"会话失效"，而不是当成解析错误
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress(0), 0);
        server.createContext("/Com/queryAssetAndPositionV1", exchange -> {
            byte[] page = ("<html><head><title>在线交易 东方财富在线交易，安全，便捷！</title></head>"
                    + "<body>login page</body></html>").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, page.length);
            exchange.getResponseBody().write(page);
            exchange.close();
        });
        server.start();
        try {
            EastmoneyJywgClient client = new EastmoneyJywgClient(new OkHttpClient(), new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), "SESSION=abc", "vk123", stockSyncRepository);
            client.updateSession("SESSION=abc", "vk123", 180);

            assertThatThrownBy(() -> client.assetAndPosition())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("交易会话已失效")
                    .hasMessageContaining("重新登录");
        } finally {
            server.stop(0);
        }
    }
}
