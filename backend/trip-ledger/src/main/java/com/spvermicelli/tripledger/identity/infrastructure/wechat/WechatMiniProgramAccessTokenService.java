package com.spvermicelli.tripledger.identity.infrastructure.wechat;

import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 微信小程序 access token 服务。
 * 这里专门负责与微信 stable_token 接口交互，并在进程内做短期缓存，
 * 避免每次绑定手机号都重新向微信申请 access token。
 */
@Component
public class WechatMiniProgramAccessTokenService {

    private static final long CACHE_SAFE_WINDOW_SECONDS = 120L;

    private final WechatMiniProgramProperties properties;
    private final RestClient restClient;

    private volatile CachedAccessToken cachedAccessToken;

    public WechatMiniProgramAccessTokenService(WechatMiniProgramProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public String getAccessToken() {
        CachedAccessToken currentCache = cachedAccessToken;
        if (currentCache != null && !currentCache.isExpired()) {
            return currentCache.token();
        }

        synchronized (this) {
            currentCache = cachedAccessToken;
            if (currentCache != null && !currentCache.isExpired()) {
                return currentCache.token();
            }

            CachedAccessToken refreshedToken = requestAccessToken();
            cachedAccessToken = refreshedToken;
            return refreshedToken.token();
        }
    }

    private CachedAccessToken requestAccessToken() {
        try {
            String responseBody = restClient.post()
                .uri(properties.getStableAccessTokenUrl())
                .body(Map.of(
                    "grant_type", "client_credential",
                    "appid", properties.getAppId(),
                    "secret", properties.getAppSecret(),
                    "force_refresh", false
                ))
                .retrieve()
                .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信 access token 服务返回为空");
            }

            AccessTokenResponse response = parseResponse(responseBody);
            if (response.errcode() != null && response.errcode() != 0) {
                throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "获取微信 access token 失败：" + defaultErrorMessage(response.errmsg())
                );
            }
            if (!StringUtils.hasText(response.accessToken())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信 access token 未返回");
            }

            long expiresIn = response.expiresIn() == null ? 0L : response.expiresIn();
            Instant expireAt = Instant.now().plusSeconds(Math.max(expiresIn - CACHE_SAFE_WINDOW_SECONDS, 60L));
            return new CachedAccessToken(response.accessToken(), expireAt);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用微信 access token 服务失败");
        }
    }

    @SuppressWarnings("unchecked")
    private AccessTokenResponse parseResponse(String responseBody) {
        try {
            Map<String, Object> payload = JsonParserFactory.getJsonParser().parseMap(responseBody);
            Integer errorCode = null;
            Object errcode = payload.get("errcode");
            if (errcode instanceof Number number) {
                errorCode = number.intValue();
            } else if (errcode instanceof String errcodeText && StringUtils.hasText(errcodeText)) {
                errorCode = Integer.parseInt(errcodeText);
            }

            Long expiresIn = null;
            Object expiresInValue = payload.get("expires_in");
            if (expiresInValue instanceof Number number) {
                expiresIn = number.longValue();
            } else if (expiresInValue instanceof String expiresInText && StringUtils.hasText(expiresInText)) {
                expiresIn = Long.parseLong(expiresInText);
            }

            return new AccessTokenResponse(
                toText(payload.get("access_token")),
                expiresIn,
                errorCode,
                toText(payload.get("errmsg"))
            );
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析微信 access token 响应失败");
        }
    }

    private String defaultErrorMessage(String errorMessage) {
        return StringUtils.hasText(errorMessage) ? errorMessage : "微信 access token 服务异常";
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record AccessTokenResponse(
        String accessToken,
        Long expiresIn,
        Integer errcode,
        String errmsg
    ) {
    }

    private record CachedAccessToken(String token, Instant expireAt) {
        private boolean isExpired() {
            return expireAt.isBefore(Instant.now());
        }
    }
}
