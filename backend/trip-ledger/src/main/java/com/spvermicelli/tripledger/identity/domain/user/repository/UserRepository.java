package com.spvermicelli.tripledger.identity.domain.user.repository;

import com.spvermicelli.tripledger.identity.domain.user.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByWechatOpenId(String wechatOpenId);

    Optional<User> findById(Long id);

    List<User> findByIds(Collection<Long> ids);

    Optional<User> findByMobile(String mobile);

    List<User> findByWechatOpenIdPrefix(String wechatOpenIdPrefix);

    List<User> searchActiveByMobileKeyword(String keyword);

    List<User> searchActiveByNicknameKeyword(String keyword);

    User save(User user);

    void deleteById(Long id);
}
