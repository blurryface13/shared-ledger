package com.spvermicelli.tripledger.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.UserMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.UserPO;
import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserMapperIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldInsertUserIntoMysql() {
        UserPO user = new UserPO();
        user.setWechatOpenId("test-openid-" + UUID.randomUUID());
        user.setNickname("integration-test-user");
        user.setStatus(UserStatus.ACTIVE);

        try {
            int affectedRows = userMapper.insert(user);

            assertThat(affectedRows).isEqualTo(1);
            assertThat(user.getId()).isNotNull();

            UserPO persistedUser = userMapper.selectById(user.getId());
            assertThat(persistedUser).isNotNull();
            assertThat(persistedUser.getNickname()).isEqualTo("integration-test-user");
            assertThat(persistedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        } finally {
            if (user.getId() != null) {
                userMapper.deleteById(user.getId());
            }
        }
    }
}
