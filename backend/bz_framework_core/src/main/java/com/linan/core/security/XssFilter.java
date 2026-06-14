package com.linan.core.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * XSS 防护过滤器
 *
 * <p>基于 Jsoup 过滤所有用户输入的 HTML/脚本内容，防止存储型 XSS 和反射型 XSS 攻击。
 * 对请求参数（query string）和请求体（JSON body）中的字符串进行 HTML 标签剥离。</p>
 *
 * <p>白名单策略：{@link Safelist#basic()} 允许基础的格式化标签（b, i, u, a, br, p 等），
 * 剥离 script, style, iframe, onclick 等危险标签和属性。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>包装 HttpServletRequest，拦截 getParameter、getParameterValues、getHeader 方法</li>
 *   <li>对于 application/json 请求体，读取完整 body 进行 JSON 字符串值过滤</li>
 *   <li>使用 Jsoup 的 Safelist.basic() 白名单策略清理 HTML</li>
 * </ol>
 *
 * @author BareZen Team
 */
@Component
@Order(2)
public class XssFilter implements Filter {

    /**
     * Jsoup 白名单：允许基础格式化标签，禁止 script/iframe/style/on* 事件属性
     */
    private static final Safelist SAFELIST = Safelist.basic()
            .removeTags("img", "embed", "object", "param")
            .addAttributes("a", "href", "target", "rel")
            .addAttributes("abbr", "title")
            .addAttributes("acronym", "title")
            .preserveRelativeLinks(true);

    /** Jackson ObjectMapper 用于 JSON 解析，线程安全 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        // 静态资源放行，不做 XSS 过滤（性能优化）
        String path = req.getRequestURI();
        if (path.startsWith("/profile/") || path.startsWith("/doc.html")
                || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars/")) {
            chain.doFilter(request, response);
            return;
        }

        // 包装请求，过滤参数和 body
        // 超过 2MB 请求体时 XssRequestWrapper 构造会抛 IOException，
        // 此处捕获并返回 413（而非默认的 500），符合 HTTP 语义且避免错误日志误导
        try {
            XssRequestWrapper wrappedRequest = new XssRequestWrapper(req);
            chain.doFilter(wrappedRequest, response);
        } catch (IOException e) {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":413,\"msg\":\"请求体过大，超过 2MB 限制\"}");
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    /**
     * XSS 请求包装器
     *
     * 重写 getParameter、getParameterValues、getHeader 方法，
     * 对获取的字符串值进行 HTML 净化。
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        /**
         * 不参与 XSS 清洗的请求头（小写）。
         * Authorization 携带的 JWT/Base64 内容可能含特殊字符，
         * 经 Jsoup 清洗后会被转义/截断导致鉴权失败，必须原样透传。
         */
        private static final Set<String> SKIP_CLEAN_HEADERS = Set.of(
                "authorization", "cookie", "x-access-token", "content-type",
                "accept", "origin", "referer", "host", "user-agent"
        );

        /**
         * JSON 中不参与 XSS 清洗的字段名（小写）。
         *
         * 密码、令牌等敏感字段必须原样透传：Jsoup 清洗会将 {@code &}、{@code <} 等
         * 字符转义为 HTML 实体（如 {@code &amp;}），导致 BCrypt 哈希对错误字符串
         * 计算，造成用户密码被"悄悄改写"而无法登录。故这些字段跳过清洗。
         */
        private static final Set<String> SKIP_CLEAN_FIELDS = Set.of(
                "password", "oldpassword", "newpassword", "confirmpassword",
                "token", "accesstoken", "refreshtoken", "secret", "secretkey",
                "apikey", "accesskey"
        );

        private byte[] bodyBytes; // 缓存的请求体

        /** JSON 请求体大小上限：2MB，超过此限制返回 413 */
        private static final int MAX_BODY_SIZE = 2 * 1024 * 1024;

        public XssRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);

            // 仅缓存 application/json 类型的请求体
            String contentType = request.getContentType();
            if (contentType != null && contentType.startsWith("application/json")) {
                // 检查 Content-Length，防止内存耗尽攻击
                int contentLength = request.getContentLength();
                if (contentLength > MAX_BODY_SIZE) {
                    throw new IOException("Request body too large: " + contentLength + " bytes (max: " + MAX_BODY_SIZE + ")");
                }
                
                try (InputStream is = request.getInputStream();
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                        // 实时检查已读取字节数，防止无 Content-Length 的攻击
                        if (baos.size() > MAX_BODY_SIZE) {
                            throw new IOException("Request body exceeded maximum size of " + MAX_BODY_SIZE + " bytes");
                        }
                    }
                    String originalBody = baos.toString(StandardCharsets.UTF_8);
                    String cleanedBody = cleanJsonValues(originalBody);
                    this.bodyBytes = cleanedBody.getBytes(StandardCharsets.UTF_8);
                }
            }
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return cleanString(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = cleanString(values[i]);
            }
            return cleaned;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            // 安全/协议相关头不做清洗，避免破坏 JWT 等合法内容
            if (name != null && SKIP_CLEAN_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                return value;
            }
            return cleanString(value);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            // Servlet 规范要求 getParameterMap 返回不可修改 Map，
            // 直接原地修改其值数组在部分容器下会抛异常，这里返回清洗后的副本
            Map<String, String[]> map = super.getParameterMap();
            Map<String, String[]> cleanedMap = new HashMap<>(map.size());
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String[] values = entry.getValue();
                if (values == null) {
                    cleanedMap.put(entry.getKey(), null);
                    continue;
                }
                String[] cleaned = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    cleaned[i] = cleanString(values[i]);
                }
                cleanedMap.put(entry.getKey(), cleaned);
            }
            return cleanedMap;
        }

        @Override
        public ServletInputStream getInputStream() {
            if (bodyBytes != null) {
                return new CachedBodyServletInputStream(bodyBytes);
            }
            try {
                return super.getInputStream();
            } catch (IOException e) {
                // 返回空流而非 null，避免下游读取时抛 NPE
                return new CachedBodyServletInputStream(new byte[0]);
            }
        }

        @Override
        public BufferedReader getReader() {
            if (bodyBytes != null) {
                return new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(bodyBytes), StandardCharsets.UTF_8));
            }
            try {
                return super.getReader();
            } catch (IOException e) {
                // 返回空 Reader 而非 null，避免下游读取时抛 NPE
                return new BufferedReader(new StringReader(""));
            }
        }

        /**
         * 对 JSON 字符串中的所有字符串值进行 XSS 过滤
         * 使用 Jackson ObjectMapper 解析 JSON 树，递归遍历所有字符串值节点进行清洗
         */
        private String cleanJsonValues(String json) {
            if (json == null || json.isEmpty()) {
                return json;
            }
            try {
                JsonNode root = OBJECT_MAPPER.readTree(json);
                cleanJsonNode(root);
                return OBJECT_MAPPER.writeValueAsString(root);
            } catch (Exception e) {
                // JSON 解析失败，回退到逐字符处理方式
                return fallbackCleanJsonValues(json);
            }
        }

        /**
         * 递归清洗 JSON 节点中的所有字符串值
         */
        private void cleanJsonNode(JsonNode node) {
            if (node == null) {
                return;
            }
            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                Iterator<String> fieldNames = objectNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode child = objectNode.get(fieldName);
                    // 敏感字段（密码/令牌等）原样透传，跳过清洗
                    if (fieldName != null && SKIP_CLEAN_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    if (child.isTextual()) {
                        String cleaned = cleanString(child.asText());
                        objectNode.set(fieldName, new TextNode(cleaned));
                    } else {
                        cleanJsonNode(child);
                    }
                }
            } else if (node.isArray()) {
                ArrayNode arrayNode = (ArrayNode) node;
                for (int i = 0; i < arrayNode.size(); i++) {
                    JsonNode child = arrayNode.get(i);
                    if (child.isTextual()) {
                        String cleaned = cleanString(child.asText());
                        arrayNode.set(i, new TextNode(cleaned));
                    } else {
                        cleanJsonNode(child);
                    }
                }
            }
        }

        /**
         * 回退方案：原逐字符 JSON 清洗（在 Jackson 解析失败时使用）
         */
        private String fallbackCleanJsonValues(String json) {
            StringBuilder sb = new StringBuilder();
            boolean inString = false;
            StringBuilder currentValue = new StringBuilder();
            char quote = '"';

            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                if (inString) {
                    if (c == quote && (i == 0 || json.charAt(i - 1) != '\\')) {
                        String cleaned = cleanString(currentValue.toString());
                        sb.append(quote).append(cleaned).append(quote);
                        currentValue = new StringBuilder();
                        inString = false;
                    } else {
                        currentValue.append(c);
                    }
                } else {
                    if (c == quote) {
                        inString = true;
                        currentValue = new StringBuilder();
                    } else {
                        sb.append(c);
                    }
                }
            }
            if (inString) {
                sb.append(quote).append(currentValue);
            }
            return sb.toString();
        }

        /**
         * 使用 Jsoup 清理单个字符串值中的 HTML
         */
        private String cleanString(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }
            // 使用 Jsoup 清除 HTML 标签，保留纯文本
            return Jsoup.clean(value, SAFELIST);
        }
    }

    /**
     * 缓存请求体的 ServletInputStream 实现
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // not needed for synchronous reading
        }
    }
}
