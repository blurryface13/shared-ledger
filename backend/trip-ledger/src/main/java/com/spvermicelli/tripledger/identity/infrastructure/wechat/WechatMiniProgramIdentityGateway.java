package com.spvermicelli.tripledger.identity.infrastructure.wechat;

import com.spvermicelli.tripledger.identity.domain.auth.model.WechatIdentity;
import com.spvermicelli.tripledger.identity.domain.auth.service.WechatIdentityGateway;
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
 * 微信小程序身份解析实现。
 * 生产环境下会调用微信 code2Session 接口换取 openid/unionid；
 * 开发和测试环境可切换为 mock 模式，以便在本地不依赖真实微信密钥仍可完成联调。
 *
 * 后续如果要支持 App 外部拉起微信登录，可以保留当前接口不变，
 * 在 identity.infrastructure.wechat 下继续新增 App 场景的解析实现。
 */
@Component
public class WechatMiniProgramIdentityGateway implements WechatIdentityGateway {

    private final WechatMiniProgramProperties properties;
    private final RestClient restClient;

    public WechatMiniProgramIdentityGateway(WechatMiniProgramProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * 根据登陆code，解析WeChat openid和WeChat union id
     * @param code: 微信登陆code
     * @return WechatIdentity: WeChat open id & union id
     */
    @Override
    public WechatIdentity resolveMiniProgramIdentity(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "微信登录 code 不能为空");
        }

        if (properties.isMockEnabled()) {
            return new WechatIdentity("mock-openid-" + code, null);
        }

        // 验证小程序 id和secretKey是否配置
        validateWechatConfiguration();
        try {
            String responseBody = restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.getCode2SessionUrl())
                    .queryParam("appid", properties.getAppId())
                    .queryParam("secret", properties.getAppSecret())
                    .queryParam("js_code", code)
                    .queryParam("grant_type", "authorization_code")
                    .toUriString())
                .retrieve()
                .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录服务返回为空");
            }
            WechatCode2SessionResponse response = parseResponse(responseBody);
            if (response.errcode() != null && response.errcode() != 0) {
                throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "微信登录失败：" + defaultErrorMessage(response.errmsg())
                );
            }
            if (!StringUtils.hasText(response.openid())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录未返回 openid");
            }
            // 只校验是否返回open id，对union id不强制要求
            return new WechatIdentity(response.openid(), response.unionid());
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用微信登录服务失败");
        }
    }

    /**
     * 验证小程序id和app Secret是否配置
     */
    private void validateWechatConfiguration() {
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未配置微信小程序 appId 或 appSecret");
        }
    }

    private String defaultErrorMessage(String errorMessage) {
        return StringUtils.hasText(errorMessage) ? errorMessage : "微信登录服务异常";
    }

    @SuppressWarnings("unchecked")
    private WechatCode2SessionResponse parseResponse(String responseBody) {
        try {
            Map<String, Object> payload = JsonParserFactory.getJsonParser().parseMap(responseBody);
            Integer errorCode = null;
            Object errcode = payload.get("errcode");
            if (errcode instanceof Number number) {
                errorCode = number.intValue();
            } else if (errcode instanceof String errcodeText && StringUtils.hasText(errcodeText)) {
                errorCode = Integer.parseInt(errcodeText);
            }

            return new WechatCode2SessionResponse(
                toText(payload.get("openid")),
                toText(payload.get("unionid")),
                errorCode,
                toText(payload.get("errmsg"))
            );
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析微信登录响应失败");
        }
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 微信 code2Session 返回报文。
     * 这里只保留当前业务需要的字段，避免把外部接口字段扩散到领域层。
     */
    private record WechatCode2SessionResponse(
        String openid,
        String unionid,
        Integer errcode,
        String errmsg
    ) {
    }
}
