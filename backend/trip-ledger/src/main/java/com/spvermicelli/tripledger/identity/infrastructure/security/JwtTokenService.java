package com.spvermicelli.tripledger.identity.infrastructure.security;

import com.spvermicelli.tripledger.identity.domain.auth.valueobject.AccessTokenType;
import com.spvermicelli.tripledger.shared.common.context.LoginUser;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private final AuthProperties authProperties;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * 生成正式访问令牌。
     * 该令牌可访问除登录接口之外的所有受保护接口。
     */
    public JwtIssuedToken generateAccessToken(Long userId) {
        return generateToken(userId, AccessTokenType.ACCESS, authProperties.getAccessTokenExpireSeconds());
    }

    /**
     * 生成手机号绑定专用临时令牌。
     * 该令牌只允许访问“绑定微信手机号”接口，避免首登未绑定手机号场景提前拿到正式访问权限。
     */
    public JwtIssuedToken generateBindToken(Long userId) {
        return generateToken(userId, AccessTokenType.BIND_MOBILE, authProperties.getBindTokenExpireSeconds());
    }

    /**
     * 生成token函数，根据userId，tokenType，过期时间生成token
     * @param userId: 用户id
     * @param tokenType: token类型
     * @param expireSeconds: 过期时间。秒
     * @return JwtIssuedToken: token对象，含过期时间和有效时间
     */
    private JwtIssuedToken generateToken(Long userId, AccessTokenType tokenType, long expireSeconds) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);
        String token = Jwts.builder()
            .issuer(authProperties.getJwtIssuer())
            .subject(String.valueOf(userId))
            .id(UUID.randomUUID().toString())
            .claim(TOKEN_TYPE_CLAIM, tokenType.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expireAt))
            .signWith(getSecretKey())
            .compact();
        return new JwtIssuedToken(token, expireAt);
    }

    /**
     * 解析 JWT 并还原登录上下文。
     */
    public LoginUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            if (!authProperties.getJwtIssuer().equals(claims.getIssuer())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 签发方不正确");
            }
            return LoginUser.builder()
                .userId(Long.parseLong(claims.getSubject()))
                .tokenType(String.valueOf(claims.get(TOKEN_TYPE_CLAIM)))
                .build();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 无效或已过期");
        }
    }

    private SecretKey getSecretKey() {
        if (!StringUtils.hasText(authProperties.getJwtSecret())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "JWT 密钥未配置");
        }
        byte[] bytes = authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * JWT 签发结果。
     * 统一返回 token 本身和过期时间，便于接口层直接组装响应。
     */
    public record JwtIssuedToken(String token, Instant expireAt) {
    }
}
