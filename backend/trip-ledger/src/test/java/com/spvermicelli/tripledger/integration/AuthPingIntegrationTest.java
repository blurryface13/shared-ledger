package com.spvermicelli.tripledger.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.RefreshTokenSessionMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.UserMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.RefreshTokenSessionPO;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.UserPO;
import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.enabled=true")
class AuthPingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefreshTokenSessionMapper refreshTokenSessionMapper;

    private String createdWechatOpenId;

    @AfterEach
    void tearDown() {
        if (createdWechatOpenId == null) {
            return;
        }

        UserPO userPO = userMapper.selectOne(
            new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getWechatOpenId, createdWechatOpenId)
        );
        if (userPO != null) {
            refreshTokenSessionMapper.delete(new LambdaQueryWrapper<RefreshTokenSessionPO>()
                .eq(RefreshTokenSessionPO::getUserId, userPO.getId()));
            userMapper.deleteById(userPO.getId());
        }
    }

    @Test
    void shouldRequireTokenForPingWhenAuthEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4002));
    }

    @Test
    void shouldRequireTokenForBindWechatMobileWhenAuthEnabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/bind-wechat-mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "manualMobile": "13812345678"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4002));
    }

    @Test
    void shouldWechatLoginBindMobileRefreshAndAccessPing() throws Exception {
        String loginCode = "auth-test-code-" + UUID.randomUUID();
        createdWechatOpenId = "mock-openid-" + loginCode;
        String loginRequestBody = """
            {
              "code": "%s"
            }
            """.formatted(loginCode);

        String loginResponseBody = mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.mobileBound").value(false))
            .andExpect(jsonPath("$.data.needBindMobile").value(true))
            .andExpect(jsonPath("$.data.bindToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String bindToken = extractField(loginResponseBody, "bindToken");
        String mobile = "138" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
        String bindRequestBody = """
            {
              "phoneCode": "%s"
            }
            """.formatted(mobile);

        String bindResponseBody = mockMvc.perform(post("/api/v1/auth/bind-wechat-mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + bindToken)
                .content(bindRequestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.mobileBound").value(true))
            .andExpect(jsonPath("$.data.token").isString())
            .andExpect(jsonPath("$.data.refreshToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String token = extractField(bindResponseBody, "token");
        String refreshToken = extractField(bindResponseBody, "refreshToken");

        mockMvc.perform(get("/api/v1/ping")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.currentUserId").isNumber());

        String refreshRequestBody = """
            {
              "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").isString())
            .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void shouldDirectlyLoginWhenExistingUserHasBoundMobile() throws Exception {
        String loginCode = "existing-code-" + UUID.randomUUID();
        createdWechatOpenId = "mock-openid-" + loginCode;
        UserPO user = new UserPO();
        user.setWechatOpenId(createdWechatOpenId);
        user.setNickname("existing-user");
        user.setMobile("139" + ThreadLocalRandom.current().nextInt(1000_0000, 9999_9999));
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);

        String requestBody = """
            {
              "code": "%s"
            }
            """.formatted(loginCode);

        mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.mobileBound").value(true))
            .andExpect(jsonPath("$.data.needBindMobile").value(false))
            .andExpect(jsonPath("$.data.token").isString())
            .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void shouldUpdateCurrentUserProfileAndCancelCurrentUser() throws Exception {
        String loginCode = "profile-code-" + UUID.randomUUID();
        createdWechatOpenId = "mock-openid-" + loginCode;
        UserPO user = new UserPO();
        user.setWechatOpenId(createdWechatOpenId);
        user.setNickname("before-update");
        user.setAvatarUrl("https://old-avatar.example.com/avatar.png");
        user.setMobile("139" + ThreadLocalRandom.current().nextInt(1000_0000, 9999_9999));
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);

        String loginResponseBody = mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s"
                    }
                    """.formatted(loginCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = extractField(loginResponseBody, "token");
        String refreshToken = extractField(loginResponseBody, "refreshToken");
        String newMobile = "138" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.nickname").value("before-update"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://old-avatar.example.com/avatar.png"))
            .andExpect(jsonPath("$.data.mobile").value(user.getMobile()))
            .andExpect(jsonPath("$.data.createdAt").isString())
            .andExpect(jsonPath("$.data.updatedAt").isString());

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content("""
                    {
                      "nickname": "after-update",
                      "phoneCode": "%s"
                    }
                    """.formatted(newMobile)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.nickname").value("after-update"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://old-avatar.example.com/avatar.png"))
            .andExpect(jsonPath("$.data.mobileBound").value(true));

        UserPO updatedUser = userMapper.selectById(user.getId());
        org.assertj.core.api.Assertions.assertThat(updatedUser.getNickname()).isEqualTo("after-update");
        org.assertj.core.api.Assertions.assertThat(updatedUser.getAvatarUrl()).isEqualTo("https://old-avatar.example.com/avatar.png");
        org.assertj.core.api.Assertions.assertThat(updatedUser.getMobile()).isEqualTo(newMobile);

        mockMvc.perform(post("/api/v1/users/me/cancel")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        UserPO cancelledUser = userMapper.selectById(user.getId());
        org.assertj.core.api.Assertions.assertThat(cancelledUser.getStatus()).isEqualTo(UserStatus.CANCELLED);

        mockMvc.perform(get("/api/v1/ping")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4002));

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4002));
    }

    @Test
    void shouldRejectUsingBindTokenToUpdateCurrentUser() throws Exception {
        String loginCode = "bind-only-code-" + UUID.randomUUID();
        createdWechatOpenId = "mock-openid-" + loginCode;

        String loginResponseBody = mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s"
                    }
                    """.formatted(loginCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.bindToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String bindToken = extractField(loginResponseBody, "bindToken");

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + bindToken)
                .content("""
                    {
                      "nickname": "should-fail"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(4008));
    }

    @Test
    void shouldRequireConfirmationWhenWechatLoginHitsCancelledUser() throws Exception {
        String loginCode = "cancelled-code-" + UUID.randomUUID();
        createdWechatOpenId = "mock-openid-" + loginCode;
        UserPO user = new UserPO();
        user.setWechatOpenId(createdWechatOpenId);
        user.setNickname("cancelled-user");
        user.setStatus(UserStatus.CANCELLED);
        userMapper.insert(user);

        mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s"
                    }
                    """.formatted(loginCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.cancelledUser").value(true))
            .andExpect(jsonPath("$.data.needConfirmReRegister").value(true));
    }

    @Test
    void shouldReactivateCancelledUserWhenWechatLoginConfirmed() throws Exception {
        String loginCode = "reactivate-code-" + UUID.randomUUID();
        createdWechatOpenId = "mock-openid-" + loginCode;
        UserPO user = new UserPO();
        user.setWechatOpenId(createdWechatOpenId);
        user.setNickname("reactivate-user");
        user.setMobile("139" + ThreadLocalRandom.current().nextInt(1000_0000, 9999_9999));
        user.setStatus(UserStatus.CANCELLED);
        userMapper.insert(user);

        mockMvc.perform(post("/api/v1/auth/wechat-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s",
                      "confirmReRegister": true
                    }
                    """.formatted(loginCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.cancelledUser").value(false))
            .andExpect(jsonPath("$.data.needConfirmReRegister").value(false))
            .andExpect(jsonPath("$.data.token").isString())
            .andExpect(jsonPath("$.data.refreshToken").isString());

        UserPO reactivatedUser = userMapper.selectById(user.getId());
        org.assertj.core.api.Assertions.assertThat(reactivatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldSupportMapperEnumPersistence() {
        UserPO user = new UserPO();
        user.setWechatOpenId("enum-test-openid-" + UUID.randomUUID());
        user.setNickname("enum-user");
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);
        userMapper.deleteById(user.getId());
    }

    private String extractField(String responseBody, String fieldName) {
        String marker = "\"" + fieldName + "\":\"";
        int start = responseBody.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException(fieldName + " not found in response: " + responseBody);
        }
        int tokenStart = start + marker.length();
        int tokenEnd = responseBody.indexOf('"', tokenStart);
        return responseBody.substring(tokenStart, tokenEnd);
    }
}
