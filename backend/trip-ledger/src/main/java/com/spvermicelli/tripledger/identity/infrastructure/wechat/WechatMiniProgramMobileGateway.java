package com.spvermicelli.tripledger.identity.infrastructure.wechat;

import com.spvermicelli.tripledger.identity.domain.user.service.WechatMobileGateway;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 微信手机号解析实现。
 * 1. 小程序正式环境：后端携带 getPhoneNumber 返回的 code 调用微信手机号接口。
 * 2. 开发/测试环境：允许沿用 11 位手机号 mock，便于本地联调与自动化测试。
 * 3. 未来 App 场景：允许手动输入手机号，但仅作为补充链路，不影响当前小程序主流程。
 */
@Component
public class WechatMiniProgramMobileGateway implements WechatMobileGateway {

    private final WechatMiniProgramProperties properties;
    private final WechatMiniProgramAccessTokenService accessTokenService;
    private final RestClient restClient;

    public WechatMiniProgramMobileGateway(
        WechatMiniProgramProperties properties,
        WechatMiniProgramAccessTokenService accessTokenService
    ) {
        this.properties = properties;
        this.accessTokenService = accessTokenService;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String resolveMobile(String phoneCode, String manualMobile) {
        if (StringUtils.hasText(manualMobile)) {
            if (!properties.isManualMobileEnabled()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前环境不允许手动填写手机号，请使用微信手机号授权");
            }
            return validateAndNormalizeMobile(manualMobile);
        }
        if (!StringUtils.hasText(phoneCode)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "phoneCode 不能为空");
        }
        if (properties.isMockEnabled()) {
            return validateAndNormalizeMobile(phoneCode);
        }

        try {
            String accessToken = accessTokenService.getAccessToken();
            String responseBody = restClient.post()
                .uri(UriComponentsBuilder.fromUriString(properties.getGetPhoneNumberUrl())
                    .queryParam("access_token", accessToken)
                    .toUriString())
                .body(Map.of("code", phoneCode))
                .retrieve()
                .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信手机号服务返回为空");
            }

            WechatPhoneNumberResponse response = parseResponse(responseBody);
            if (response.errcode() != null && response.errcode() != 0) {
                throw new BusinessException(
                    ErrorCode.INVALID_PARAM,
                    "微信手机号获取失败：" + defaultErrorMessage(response.errmsg())
                );
            }
            if (!StringUtils.hasText(response.phoneNumber())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信手机号服务未返回手机号");
            }
            return validateAndNormalizeMobile(response.phoneNumber());
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用微信手机号服务失败");
        }
    }

    private String validateAndNormalizeMobile(String mobile) {
        if (!mobile.matches("^1\\d{10}$")) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "手机号格式不正确");
        }
        return mobile;
    }

    private String defaultErrorMessage(String errorMessage) {
        return StringUtils.hasText(errorMessage) ? errorMessage : "微信手机号服务异常";
    }

    @SuppressWarnings("unchecked")
    private WechatPhoneNumberResponse parseResponse(String responseBody) {
        try {
            Map<String, Object> payload = JsonParserFactory.getJsonParser().parseMap(responseBody);
            Integer errorCode = null;
            Object errcode = payload.get("errcode");
            if (errcode instanceof Number number) {
                errorCode = number.intValue();
            } else if (errcode instanceof String errcodeText && StringUtils.hasText(errcodeText)) {
                errorCode = Integer.parseInt(errcodeText);
            }

            Map<String, Object> phoneInfo = payload.get("phone_info") instanceof Map<?, ?> nestedMap
                ? (Map<String, Object>) nestedMap
                : Map.of();

            String purePhoneNumber = toText(phoneInfo.get("purePhoneNumber"));
            String phoneNumber = StringUtils.hasText(purePhoneNumber)
                ? purePhoneNumber
                : toText(phoneInfo.get("phoneNumber"));

            return new WechatPhoneNumberResponse(
                phoneNumber,
                toText(phoneInfo.get("countryCode")),
                errorCode,
                toText(payload.get("errmsg"))
            );
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析微信手机号响应失败");
        }
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record WechatPhoneNumberResponse(
        String phoneNumber,
        String countryCode,
        Integer errcode,
        String errmsg
    ) {
    }
}
