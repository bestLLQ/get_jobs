package utils;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP请求工具类
 */
@Slf4j
public class HttpUtils {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final int DEFAULT_TIMEOUT = 5000; // 默认超时时间（毫秒）

    static {
        // 可以在这里配置RestTemplate，例如添加拦截器、设置超时时间等
    }

    /**
     * 发送GET请求
     *
     * @param url          请求URL
     * @param params       请求参数
     * @param responseType 返回类型
     * @param headers      请求头
     * @return 响应结果
     */
    public static <T> T get(String url, Map<String, Object> params, Class<T> responseType, String... headers) {
        try {
            HttpHeaders httpHeaders = buildHeaders(headers);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

            // 添加请求参数
            if (params != null && !params.isEmpty()) {
                params.forEach((key, value) -> {
                    if (value != null) {
                        builder.queryParam(key, value);
                    }
                });
            }

            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.GET,
                    entity,
                    responseType);

            return response.getBody();
        } catch (RestClientException e) {
            log.error("GET请求异常: {}", url, e);
            throw e;
        }
    }

    public static <T> T get(String url, Map<String, Object> params, Class<T> responseType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

        // 添加请求参数
        if (params != null && !params.isEmpty()) {
            params.forEach((key, value) -> {
                if (value != null) {
                    builder.queryParam(key, value);
                }
            });
        }
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<T> response = restTemplate.exchange(
                builder.build().encode().toUri(),
                HttpMethod.GET,
                entity,
                responseType);

        return response.getBody();
    }

    public static <T> T get(String url, Object params, Class<T> responseType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

        // 添加请求参数
        if (params != null) {
            Map<String, Object> map = JSONObject.parseObject(JSONObject.toJSONString(params), Map.class);
            map.forEach((key, value) -> {
                if (value != null) {
                    builder.queryParam(key, value);
                }
            });
        }
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<T> response = restTemplate.exchange(
                builder.build().encode().toUri(),
                HttpMethod.GET,
                entity,
                responseType);

        return response.getBody();
    }

    public static <T> T post(String url, Object params, Class<T> responseType) {
        try {
            String urlWithParams = buildUrlWithParams(url, JSONObject.parseObject(JSONObject.toJSONString(params), Map.class));
            HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
            ResponseEntity<T> response = restTemplate.exchange(
                    urlWithParams,
                    HttpMethod.POST,
                    entity,
                    responseType);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("POST请求异常: {}", url, e);
            throw e;
        }
    }


    /**
     * 发送POST请求（表单数据）
     *
     * @param url          请求URL
     * @param params       请求参数
     * @param responseType 返回类型
     * @param headers      请求头
     * @return 响应结果
     */
    public static <T> T postForm(String url, Map<String, Object> params, Class<T> responseType, String... headers) {
        try {
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.add("Content-Type", "application/x-www-form-urlencoded");

            MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();
            if (params != null && !params.isEmpty()) {
                params.forEach(formParams::add);
            }

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(formParams, httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType);

            return response.getBody();
        } catch (RestClientException e) {
            log.error("POST表单请求异常: {}", url, e);
            throw e;
        }
    }

    /**
     * 发送POST请求（JSON数据）
     *
     * @param url          请求URL
     * @param body         请求体
     * @param responseType 返回类型
     * @param headers      请求头
     * @return 响应结果
     */
    public static <T> T postJson(String url, Object body, Class<T> responseType, String... headers) {
        try {
            log.info("发送POST JSON请求: {}", url);
            log.debug("请求体: {}", JSONObject.toJSONString(body));

            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.add("Content-Type", "application/json");

            String jsonBody = JSONObject.toJSONString(body);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, httpHeaders);

            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType);

            log.debug("响应状态: {}", response.getStatusCode());
            log.debug("响应体: {}", response.getBody());

            return response.getBody();
        } catch (RestClientException e) {
            log.error("POST JSON请求异常: {}, 错误信息: {}", url, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 发送PUT请求
     *
     * @param url          请求URL
     * @param body         请求体
     * @param responseType 返回类型
     * @param headers      请求头
     * @return 响应结果
     */
    public static <T> T put(String url, Object body, Class<T> responseType, String... headers) {
        try {
            HttpHeaders httpHeaders = buildHeaders(headers);
            HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    responseType);

            return response.getBody();
        } catch (RestClientException e) {
            log.error("PUT请求异常: {}", url, e);
            throw e;
        }
    }

    /**
     * 发送DELETE请求
     *
     * @param url          请求URL
     * @param params       请求参数
     * @param responseType 返回类型
     * @param headers      请求头
     * @return 响应结果
     */
    public static <T> T delete(String url, Map<String, Object> params, Class<T> responseType, String... headers) {
        try {
            HttpHeaders httpHeaders = buildHeaders(headers);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

            // 添加请求参数
            if (params != null && !params.isEmpty()) {
                params.forEach((key, value) -> {
                    if (value != null) {
                        builder.queryParam(key, value);
                    }
                });
            }

            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.DELETE,
                    entity,
                    responseType);

            return response.getBody();
        } catch (RestClientException e) {
            log.error("DELETE请求异常: {}", url, e);
            throw e;
        }
    }

    /**
     * 构建请求头
     *
     * @param headers 请求头数组，格式为：key1, value1, key2, value2...
     * @return HttpHeaders对象
     */
    private static HttpHeaders buildHeaders(String... headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null && headers.length > 0) {
            if (headers.length % 2 != 0) {
                throw new RuntimeException("请求头参数必须成对出现");
            }

            for (int i = 0; i < headers.length; i += 2) {
                httpHeaders.add(headers[i], headers[i + 1]);
            }
        }
        return httpHeaders;
    }

    /**
     * 构建URL参数
     *
     * @param url    基础URL
     * @param params 参数Map
     * @return 带参数的URL
     */
    public static String buildUrlWithParams(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach((key, value) -> {
            if (value != null) {
                builder.queryParam(key, value);
            }
        });

        return builder.build().encode().toUriString();
    }
}
