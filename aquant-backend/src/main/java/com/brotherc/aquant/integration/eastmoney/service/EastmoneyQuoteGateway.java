package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * 东财行情请求网关：统一持有浏览器指纹客户端、全局请求节流、单主机退避重试、实时接口跨主机切换
 * 与按主机熔断。push2/push2his 等行情主机的风控按 IP 临时封禁且会升级：先返回 502 页面，
 * 加重后直接在连接层无声掐断（TLS 收不到 close_notify / 浏览器 ERR_EMPTY_RESPONSE），
 * 封禁期内重试只会延长封禁，因此按主机做熔断冷却。
 * 实时类接口（clist、分笔明细）按 push2delay 优先、push2 兜底；历史类接口（push2his）无延迟版主机，
 * 保持单主机退避重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EastmoneyQuoteGateway {

    /** 失败日志中响应体截断长度：风控响应体（挑战页/空响应）是定位根因最直接的证据 */
    private static final int FAIL_BODY_SNIPPET = 300;
    /** 退避基数与随机抖动，避免重试节奏被识别 */
    private static final long RETRY_BACKOFF_BASE_MILLIS = 1500L;
    private static final long RETRY_BACKOFF_SPREAD_MILLIS = 1000L;

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    /**
     * 实时行情接口主机列表（逗号分隔，按优先级，仅 scheme+host，路径由调用方追加）。
     * push2delay 为 akshare 官方采用的延迟行情主机，数据滞后约 1-2 分钟，收盘后同步与
     * 板块列表展示无影响；push2 保留作兜底，防 push2delay 故障时整体不可用。
     */
    @Value("${aquant.eastmoney.quote-base-urls:https://push2delay.eastmoney.com,https://push2.eastmoney.com}")
    private List<String> quoteBaseUrls;

    /** 是否启用 TLS 指纹模拟，关闭后回落到 JDK 默认 SSLContext（排查 WAF 是否由 TLS 指纹触发） */
    @Value("${aquant.eastmoney.tls-impersonate:true}")
    private boolean tlsImpersonate;

    /** 失败重试次数（不含首次）。东财短时抖动退避后重试通常可自愈 */
    @Value("${aquant.eastmoney.max-retries:2}")
    private int maxRetries;

    /** 相邻上游请求最小间隔（毫秒）与随机浮动范围，实际间隔 = min + random(0, spread)。
     * 东财按 IP + 主机统计请求频次触发临时封禁，这是主要的调节旋钮，被封时优先调大 */
    @Value("${aquant.eastmoney.pace-min-millis:1500}")
    private long paceMinMillis;

    @Value("${aquant.eastmoney.pace-spread-millis:2000}")
    private long paceSpreadMillis;

    /** 单主机连续传输级失败达到该阈值后熔断（连接被掐断/502/空响应） */
    @Value("${aquant.eastmoney.breaker-threshold:3}")
    private int breakerThreshold;

    /** 主机熔断冷却时长（毫秒），默认 15 分钟。封禁期内发请求只会延长封禁 */
    @Value("${aquant.eastmoney.cooldown-millis:900000}")
    private long cooldownMillis;

    /**
     * 会话 Cookie 初始值（完整 Cookie 请求头字符串，可留空）。quote.eastmoney.com 首次访问触发滑块验证，
     * 通过后浏览器持有的会话 Cookie 是 push2 接口的通行证。实测有 TTL（重度使用约 30-40 分钟），
     * 失效后在浏览器重新通过滑块验证，调用 POST /eastmoney/cookie 热更新，无需重启
     */
    @Value("${aquant.eastmoney.cookie:}")
    private String configuredCookie;

    /** 会话 Cookie 当前值（支持运行期热更新，每次请求实时读取） */
    private final AtomicReference<String> sessionCookie = new AtomicReference<>();

    /** 带浏览器指纹头的专用客户端，不污染共享 OkHttpClient */
    private OkHttpClient browserClient;

    @PostConstruct
    void initBrowserClient() {
        sessionCookie.set(configuredCookie == null || configuredCookie.isBlank() ? null : configuredCookie.trim());
        browserClient = EastmoneyBrowserClientFactory.create(okHttpClient, tlsImpersonate, sessionCookie::get);
        log.info("东财行情网关已初始化: tlsImpersonate={}, maxRetries={}, pace={}~{}ms, breaker={}次/{}ms, sessionCookie={}, quoteBaseUrls={}",
                tlsImpersonate, maxRetries, paceMinMillis, paceMinMillis + Math.max(1, paceSpreadMillis),
                breakerThreshold, cooldownMillis, sessionCookie.get() == null ? "未配置" : "已配置",
                quoteBaseUrls);
    }

    /**
     * 热更新会话 Cookie（浏览器重新通过滑块验证后，从 DevTools 复制完整 Cookie 调用），
     * 立即对后续请求生效，无需重启。传空白视为清除。
     * 同时重置主机熔断状态：更新 Cookie 意味着人工已重新通过验证，新凭证应立即接受检验，
     * 否则冷却期内请求根本不会发出，更新的 Cookie 永远没有机会生效
     */
    public void updateSessionCookie(String cookie) {
        String normalized = cookie == null || cookie.isBlank() ? null : cookie.trim();
        sessionCookie.set(normalized);
        cooldownUntilByHost.clear();
        failureStreakByHost.values().forEach(streak -> streak.set(0));
        log.info("东财会话 Cookie 已热更新({})，主机熔断状态已重置",
                normalized == null ? "已清空" : "len=" + normalized.length());
    }

    public boolean isSessionCookieConfigured() {
        String cookie = sessionCookie.get();
        return cookie != null && !cookie.isBlank();
    }

    /** 指定主机是否处于熔断冷却中 */
    public boolean isCoolingDown(String host) {
        Long until = cooldownUntilByHost.get(host);
        return until != null && System.currentTimeMillis() < until;
    }

    /** 任意已见主机是否仍在熔断冷却中（用于调用方快速放弃本轮，避免逐板块空转阻塞线程） */
    public boolean anyHostCoolingDown() {
        long now = System.currentTimeMillis();
        for (Long until : cooldownUntilByHost.values()) {
            if (until != null && now < until) {
                return true;
            }
        }
        return false;
    }

    private final Object paceLock = new Object();
    private long lastRequestAtMillis = 0L;

    /** 最近一次实时接口成功主机。分页/循环拉取固定走已验证主机，避免每页都先撞一遍坏主机 */
    private volatile String preferredQuoteBaseUrl;

    /** 各主机连续传输级失败计数 */
    private final ConcurrentHashMap<String, AtomicInteger> failureStreakByHost = new ConcurrentHashMap<>();

    /** 各主机熔断冷却截止时间戳 */
    private final ConcurrentHashMap<String, Long> cooldownUntilByHost = new ConcurrentHashMap<>();

    /**
     * 请求节流：所有东财上游请求共享同一节奏预算，并发调用方（同步任务/分时查询）串行排队
     */
    private void pace() {
        long interval = paceMinMillis + ThreadLocalRandom.current().nextLong(Math.max(1L, paceSpreadMillis));
        synchronized (paceLock) {
            long waitMillis = lastRequestAtMillis + interval - System.currentTimeMillis();
            if (waitMillis > 0) {
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestAtMillis = System.currentTimeMillis();
        }
    }

    /**
     * 执行实时行情接口请求（clist/分笔明细等），按主机优先级切换，跳过熔断中的主机。
     * push2 的封禁按 IP 统计且封禁期内同一主机退避重试无法恢复，必须换主机或等待冷却。
     *
     * @param customizer 在主机基础上追加路径与查询参数
     */
    public JsonNode executeQuote(UnaryOperator<HttpUrl.Builder> customizer) {
        List<String> baseUrls = new ArrayList<>(quoteBaseUrls);
        if (preferredQuoteBaseUrl != null && baseUrls.remove(preferredQuoteBaseUrl)) {
            baseUrls.add(0, preferredQuoteBaseUrl);
        }
        BusinessException lastFailure = null;
        boolean allCoolingDown = true;
        for (String baseUrl : baseUrls) {
            HttpUrl url = HttpUrl.get(baseUrl).newBuilder().build();
            if (isCoolingDown(url.host())) {
                log.info("东财行情主机熔断冷却中，跳过: host={}", url.host());
                continue;
            }
            allCoolingDown = false;
            try {
                JsonNode root = execute(customizer.apply(HttpUrl.get(baseUrl).newBuilder()).build());
                preferredQuoteBaseUrl = baseUrl;
                return root;
            } catch (BusinessException e) {
                lastFailure = e;
                log.warn("东财行情主机失败，切换下一主机: host={}", baseUrl);
            }
        }
        if (allCoolingDown) {
            throw new EastmoneyCoolingDownException("所有东财行情主机均在熔断冷却中: " + quoteBaseUrls);
        }
        throw lastFailure == null ? ExceptionEnum.API_REQUEST_ERROR.toException() : lastFailure;
    }

    /**
     * 执行一次上游请求，失败按退避策略重试。主机熔断冷却中直接失败（不发请求）。
     */
    public JsonNode execute(HttpUrl url) {
        if (isCoolingDown(url.host())) {
            log.info("东财行情主机熔断冷却中，直接失败(未发请求): host={}, 剩余={}ms",
                    url.host(), remainingCooldownMillis(url.host()));
            throw new EastmoneyCoolingDownException(url.host(), remainingCooldownMillis(url.host()));
        }
        int maxAttempts = Math.max(1, maxRetries + 1);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeOnce(url);
            } catch (BusinessException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                long backoff = RETRY_BACKOFF_BASE_MILLIS * attempt
                        + ThreadLocalRandom.current().nextLong(RETRY_BACKOFF_SPREAD_MILLIS);
                log.warn("东财行情接口第{}次失败，{}ms 后重试: url={}", attempt, backoff, url);
                sleepQuietly(backoff);
            }
        }
        // 不可达：maxAttempts >= 1 时循环必在首次成功返回或最后一次抛出
        throw ExceptionEnum.API_REQUEST_ERROR.toException();
    }

    private JsonNode executeOnce(HttpUrl url) {
        pace();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = browserClient.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful() || body.isBlank()) {
                // 空响应体（HTTP 200 + 0 字节）与 502 页面同属传输级失败：东财边缘节点按 IP
                // 临时封禁的两种形态（502 页面 → 无声掐断），必须计数熔断而非无脑重试
                log.warn("东财行情接口响应异常: status={}, protocol={}, server={}, setCookie={}, bodyLen={}, url={}, body={}",
                        response.code(), response.protocol(), response.header("Server"),
                        response.headers("Set-Cookie"), body.length(), url, snippet(body));
                countTransportFailure(url.host(), "status=" + response.code() + ", bodyLen=" + body.length());
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }
            JsonNode root;
            try {
                root = objectMapper.readTree(body);
            } catch (IOException e) {
                // 响应可读但非 JSON，属数据问题而非传输问题，不计入熔断
                log.error("东财行情接口响应解析失败, url={}", url, e);
                throw ExceptionEnum.API_REQUEST_ERROR.toException(e);
            }
            if (root.path("rc").asInt(-1) != 0) {
                log.warn("东财行情接口业务失败: rc={}, url={}, body={}", root.path("rc").asInt(-1), url, snippet(body));
                throw ExceptionEnum.API_REQUEST_ERROR.toException();
            }
            failureStreakByHost.computeIfAbsent(url.host(), host -> new AtomicInteger()).set(0);
            return root;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            // 连接被服务端/中间盒无声掐断（TLS 无 close_notify / 读响应头时 EOF）：
            // 东财边缘节点按 IP 临时封禁的典型形态，浏览器同 IP 同样打不开
            log.error("东财行情接口连接被掐断, url={}", url, e);
            countTransportFailure(url.host(), e.getClass().getSimpleName());
            throw ExceptionEnum.API_REQUEST_ERROR.toException(e);
        } catch (Exception e) {
            log.error("东财行情接口请求失败, url={}", url, e);
            throw ExceptionEnum.API_REQUEST_ERROR.toException(e);
        }
    }

    private long remainingCooldownMillis(String host) {
        Long until = cooldownUntilByHost.get(host);
        return until == null ? 0 : Math.max(0, until - System.currentTimeMillis());
    }

    /**
     * 传输级失败计数：连续达到阈值即对该主机熔断冷却。
     * 封禁是东财边缘节点按 IP 做的，冷却期内继续发请求只会刷新封禁窗口
     */
    private void countTransportFailure(String host, String detail) {
        int streak = failureStreakByHost.computeIfAbsent(host, key -> new AtomicInteger()).incrementAndGet();
        if (streak >= Math.max(1, breakerThreshold)) {
            cooldownUntilByHost.put(host, System.currentTimeMillis() + cooldownMillis);
            failureStreakByHost.get(host).set(0);
            log.error("东财行情主机连续 {} 次传输级失败（{}），判定为 IP 级临时封禁，熔断 {}ms，冷却期内不再向该主机发请求: host={}{}",
                    streak, detail, cooldownMillis, host,
                    isSessionCookieConfigured()
                            ? "。会话 Cookie 可能已失效（实测有 TTL），请在浏览器重新通过滑块验证后调用 POST /eastmoney/cookie 热更新"
                            : "");
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() <= FAIL_BODY_SNIPPET ? flat : flat.substring(0, FAIL_BODY_SNIPPET) + "...";
    }
}
