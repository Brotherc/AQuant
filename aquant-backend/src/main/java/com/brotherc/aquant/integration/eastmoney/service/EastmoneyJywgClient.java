package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 东方财富证券网页交易（jywg.eastmoneysec.com，原 jywg.18.cn）只读查询客户端。
 *
 * <p>不做登录自动化（滑块验证 + 密码 RSA + 图形验证码逆向成本高且易碎），复用项目既有的
 * 「东财 Cookie 热更新」模式：用户在浏览器完成 jywg 登录后，从 DevTools 复制
 * 会话 Cookie 与 validatekey（任意查询请求 URL 参数，或页面 #em_validatekey 的 value），
 * 调 POST /portfolio/jywg/cookie 热更新并选择在线时长（15/30/180 分钟，对应 jywg 登录
 * duration 语义），前端按过期时间倒计时。仅实现只读查询（持仓/资产/当日成交/资金流水），
 * 不封装下单等写操作。</p>
 *
 * <p>接口契约参考 wmo-v/eastmoneyapi（Go 逆向实现）：
 * 查询类 GET 端点统一带 {@code validatekey} 参数，响应为 {@code {Status:0, Data:[...], Message}}，
 * 字段名为拼音首字母（Zqdm 证券代码、Zqmc 证券名称、Cjjg 成交价格、Cjsl 成交数量、
 * Cjbh 成交编号、Kysl 可用数量、Zqsl 持仓数量、Cbjg 成本价等）。</p>
 *
 * <p>注意：这里的交易会话与行情中心的东财 Cookie（{@code aquant.eastmoney.cookie}，
 * 作用于 push2 行情接口）是两个完全独立的凭证，互不通用。</p>
 */
@Slf4j
@Service
public class EastmoneyJywgClient {

    /** 在线时长可选项（分钟），对齐 jywg 登录 duration 语义 */
    public static final Set<Integer> ALLOWED_DURATIONS = Set.of(15, 30, 180);

    /** 会话在 stock_sync 表中的持久化键：记录更新时间与在线时长，重启后倒计时可续算 */
    public static final String SESSION_STORAGE_KEY = "portfolio_jywg_session";

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final StockSyncRepository stockSyncRepository;

    /**
     * 当前会话。expireAtMillis 为空表示未计时（如来自静态配置且尚未热更新过），
     * 到期后查询直接拒绝并提示重新获取（上游 Cookie 同步过期）。
     */
    private volatile Session session;

    private record Session(String cookie, String validateKey, Long expireAtMillis,
                           Integer durationMinutes, Long updateTimeMillis) {
    }

    public EastmoneyJywgClient(OkHttpClient okHttpClient, ObjectMapper objectMapper,
                               @Value("${aquant.portfolio.jywg-base-url:https://jywg.eastmoneysec.com}") String baseUrl,
                               @Value("${aquant.portfolio.jywg-cookie:}") String cookie,
                               @Value("${aquant.portfolio.jywg-validate-key:}") String validateKey,
                               StockSyncRepository stockSyncRepository) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = trimToEmpty(baseUrl);
        this.stockSyncRepository = stockSyncRepository;
        // 启动恢复顺序：优先用上次热更新持久化的会话（更新时间+时长可续算倒计时），
        // 其次回落到静态配置（不计时）。两者都无则视为未配置
        Session restored = restorePersistedSession();
        if (restored != null) {
            this.session = restored;
            log.info("东财网页交易会话已从持久化记录恢复: updateTime={}, duration={}min",
                    Instant.ofEpochMilli(restored.updateTimeMillis()), restored.durationMinutes());
            return;
        }
        String configuredCookie = trimToEmpty(cookie);
        String configuredValidateKey = trimToEmpty(validateKey);
        this.session = configuredCookie.isBlank() || configuredValidateKey.isBlank() ? null
                : new Session(configuredCookie, configuredValidateKey, null, null, null);
        if (this.session != null) {
            log.info("东财网页交易会话已从配置初始化（未计时，倒计时以热更新为准）");
        }
    }

    /** 从 stock_sync 读取上次热更新的会话（Cookie/validatekey/更新时间/时长），倒计时按 updateTime+duration 续算 */
    private Session restorePersistedSession() {
        try {
            StockSync stored = stockSyncRepository.findByName(SESSION_STORAGE_KEY);
            if (stored == null || StringUtils.isBlank(stored.getValue())) {
                return null;
            }
            JsonNode node = objectMapper.readTree(stored.getValue());
            String cookie = node.path("cookie").asText("");
            String validateKey = node.path("validateKey").asText("");
            long updateTime = node.path("updateTime").asLong(0);
            int durationMinutes = node.path("durationMinutes").asInt(0);
            if (cookie.isBlank() || validateKey.isBlank() || updateTime <= 0 || durationMinutes <= 0) {
                return null;
            }
            return new Session(cookie, validateKey, updateTime + durationMinutes * 60_000L,
                    durationMinutes, updateTime);
        } catch (Exception exception) {
            log.warn("东财网页交易会话持久化记录恢复失败，忽略", exception);
            return null;
        }
    }

    private void persistSession(Session current) {
        try {
            StockSync stored = stockSyncRepository.findByName(SESSION_STORAGE_KEY);
            if (stored == null) {
                stored = new StockSync();
                stored.setName(SESSION_STORAGE_KEY);
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cookie", current.cookie());
            payload.put("validateKey", current.validateKey());
            payload.put("durationMinutes", current.durationMinutes());
            payload.put("updateTime", current.updateTimeMillis());
            stored.setValue(objectMapper.writeValueAsString(payload));
            stockSyncRepository.save(stored);
        } catch (RuntimeException | IOException exception) {
            // 持久化失败不影响本次热更新生效，仅重启后倒计时/凭证不可恢复
            log.warn("东财网页交易会话持久化失败（重启后需重新设置）", exception);
        }
    }

    public boolean isConfigured() {
        return session != null;
    }

    /** 热更新交易会话：立即生效，无需重启。durationMinutes 由控制器校验取值范围（15/30/180） */
    public void updateSession(String cookie, String validateKey, int durationMinutes) {
        String normalizedCookie = trimToEmpty(cookie);
        String normalizedValidateKey = trimToEmpty(validateKey);
        if (normalizedCookie.isBlank() || normalizedValidateKey.isBlank()) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR, "Cookie 与 validatekey 均不能为空");
        }
        long updateTime = System.currentTimeMillis();
        session = new Session(normalizedCookie, normalizedValidateKey,
                updateTime + durationMinutes * 60_000L, durationMinutes, updateTime);
        persistSession(session);
        log.info("东财网页交易会话已更新: duration={}min, expireAt={}", durationMinutes,
                Instant.ofEpochMilli(session.expireAtMillis()));
    }

    /** 会话状态（前端倒计时数据源） */
    public Map<String, Object> sessionStatus() {
        Session current = session;
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", current != null);
        if (current == null) {
            status.put("expired", false);
            status.put("remainingSeconds", 0);
            status.put("updateTime", null);
            return status;
        }
        boolean timed = current.expireAtMillis() != null;
        boolean expired = timed && System.currentTimeMillis() >= current.expireAtMillis();
        status.put("expired", expired);
        status.put("remainingSeconds", timed && !expired
                ? Math.max(0, (current.expireAtMillis() - System.currentTimeMillis()) / 1000) : 0);
        status.put("durationMinutes", current.durationMinutes());
        status.put("expireAt", timed ? Instant.ofEpochMilli(current.expireAtMillis()).toString() : null);
        status.put("updateTime", current.updateTimeMillis() == null
                ? null : Instant.ofEpochMilli(current.updateTimeMillis()).toString());
        return status;
    }

    private boolean usable() {
        Session current = session;
        if (current == null) {
            return false;
        }
        return current.expireAtMillis() == null || System.currentTimeMillis() < current.expireAtMillis();
    }

    // ==================== 查询接口（契约来自 jywg 页面实际抓包，均为 POST form） ====================

    /** 单次查询行数。浏览器页面默认 20，放大以避免漏单（成交多于 20 笔时翻页参数未知） */
    private static final String QUERY_ROW_COUNT = "500";

    /** 资金余额 + 持仓明细（多证券）。form: moneyType=RMB；Data[0] = {Zzc 总资产, Kyzj 可用资金, positions: [持仓]} */
    public JsonNode assetAndPosition() {
        return postForm("/Com/queryAssetAndPositionV1", Map.of("moneyType", "RMB"));
    }

    /** 当日委托（多证券）。form: qqhs=行数&dwc= */
    public JsonNode todayOrders() {
        return postForm("/Search/queryTodayOrderWEB", form("qqhs", QUERY_ROW_COUNT, "dwc", ""));
    }

    /** 当日成交（多证券）。form: qqhs=行数&dwc= */
    public JsonNode todayMatches() {
        return postForm("/Search/queryTodayMatchWEB", form("qqhs", QUERY_ROW_COUNT, "dwc", ""));
    }

    /** 历史委托（多证券）。日期格式 yyyyMMdd；form: strdate&enddate&count&poststr= */
    public JsonNode historyOrders(String beginYmd, String endYmd) {
        return postForm("/Search/queryHisOrderMergeWEB",
                form("strdate", beginYmd, "enddate", endYmd, "count", QUERY_ROW_COUNT, "poststr", ""));
    }

    /** 历史成交（多证券）。日期格式 yyyyMMdd；form: strdate&enddate&count&poststr= */
    public JsonNode historyMatches(String beginYmd, String endYmd) {
        return postForm("/Search/queryHisMatchMergeWEB",
                form("strdate", beginYmd, "enddate", endYmd, "count", QUERY_ROW_COUNT, "poststr", ""));
    }

    /** 交割单（资金流水，含费用明细）。日期格式 yyyy-MM-dd；form: st&et&qqhs&dwc= */
    public JsonNode fundsFlow(String beginDate, String endDate) {
        return postForm("/Search/GetFundsFlow",
                form("st", beginDate, "et", endDate, "qqhs", QUERY_ROW_COUNT, "dwc", ""));
    }

    private static Map<String, String> form(String... pairs) {
        Map<String, String> form = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            form.put(pairs[i], pairs[i + 1]);
        }
        return form;
    }

    /** POST 表单查询：validatekey 走 URL 参数（与浏览器一致），业务参数走 form body */
    private JsonNode postForm(String path, Map<String, String> form) {
        Session current = session;
        if (current == null) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    "未配置东方财富网页交易会话，请先设置交易 Cookie 与 validatekey");
        }
        if (!usable()) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    "交易会话已过期（在线时长已到，上游 Cookie 同步过期），请在浏览器重新登录 jywg 并更新交易 Cookie");
        }
        okhttp3.HttpUrl url = okhttp3.HttpUrl.get(baseUrl + path).newBuilder()
                .addQueryParameter("validatekey", current.validateKey())
                .build();
        okhttp3.FormBody.Builder body = new okhttp3.FormBody.Builder();
        form.forEach(body::add);
        // 请求头对齐浏览器 XHR：jywg 部分校验会校验 X-Requested-With/User-Agent，
        // 缺失时即使 Cookie 有效也可能返回登录页 HTML
        Request request = new Request.Builder()
                .url(url)
                .header("Cookie", current.cookie())
                .header("Referer", baseUrl + "/Search/Position")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .post(body.build())
                .build();
        return execute(path, request);
    }

    private JsonNode execute(String path, Request request) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                log.warn("东财网页交易接口失败: path={}, status={}, bodyLen={}", path, response.code(), body.length());
                throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                        "东方财富网页交易接口调用失败：HTTP " + response.code() + "，会话可能已过期，请重新复制 Cookie 与 validatekey");
            }
            // 会话失效时 jywg 返回 200 + 登录页 HTML（而非 JSON/错误码），据此精确识别并指引重新登录
            if (body.stripLeading().startsWith("<")) {
                log.warn("东财网页交易接口返回登录页（会话失效）: path={}, bodyLen={}, title={}",
                        path, body.length(), bodySnippet(body));
                throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                        "交易会话已失效（Cookie 过期或 validatekey 不匹配，上游返回了登录页）。"
                                + "请在浏览器重新登录 jywg，然后更新交易 Cookie 与 validatekey");
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode status = root.path("Status");
            if (!status.isMissingNode() && !status.isNull() && status.asInt(-1) != 0) {
                log.warn("东财网页交易接口业务失败: path={}, message={}, body={}",
                        path, root.path("Message").asText(""), bodySnippet(body));
                throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                        "东方财富网页交易接口失败：" + root.path("Message").asText("Status=" + status.asInt()));
            }
            return root.path("Data");
        } catch (IOException e) {
            log.warn("东财网页交易接口不可达: path={}, base={}", path, baseUrl, e);
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    "东方财富网页交易接口不可达：" + e.getMessage());
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String bodySnippet(String body) {
        String flat = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        return flat.length() <= 200 ? flat : flat.substring(0, 200) + "...";
    }
}
