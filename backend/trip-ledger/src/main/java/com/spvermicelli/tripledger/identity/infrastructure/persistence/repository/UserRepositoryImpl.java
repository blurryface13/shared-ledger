package com.spvermicelli.tripledger.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.identity.domain.user.model.User;
import com.spvermicelli.tripledger.identity.domain.user.repository.UserRepository;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.UserMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.UserPO;
import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findByWechatOpenId(String wechatOpenId) {
        LambdaQueryWrapper<UserPO> queryWrapper = new LambdaQueryWrapper<UserPO>()
            .eq(UserPO::getWechatOpenId, wechatOpenId);
        return Optional.ofNullable(userMapper.selectOne(queryWrapper)).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<User> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectList(new LambdaQueryWrapper<UserPO>().in(UserPO::getId, ids))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<User> findByMobile(String mobile) {
        LambdaQueryWrapper<UserPO> queryWrapper = new LambdaQueryWrapper<UserPO>()
            .eq(UserPO::getMobile, mobile);
        return Optional.ofNullable(userMapper.selectOne(queryWrapper)).map(this::toDomain);
    }

    @Override
    public List<User> findByWechatOpenIdPrefix(String wechatOpenIdPrefix) {
        return userMapper.selectList(new LambdaQueryWrapper<UserPO>()
                .likeRight(UserPO::getWechatOpenId, wechatOpenIdPrefix)
                .orderByDesc(UserPO::getUpdatedAt)
                .orderByDesc(UserPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<User> searchActiveByMobileKeyword(String keyword) {
        return userMapper.selectList(new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getStatus, UserStatus.ACTIVE)
                .like(UserPO::getMobile, keyword)
                .orderByDesc(UserPO::getUpdatedAt)
                .orderByDesc(UserPO::getId)
                .last("LIMIT 20"))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<User> searchActiveByNicknameKeyword(String keyword) {
        return userMapper.selectList(new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getStatus, UserStatus.ACTIVE)
                .like(UserPO::getNickname, keyword)
                .orderByDesc(UserPO::getUpdatedAt)
                .orderByDesc(UserPO::getId)
                .last("LIMIT 20"))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public User save(User user) {
        UserPO userPO = toPO(user);
        if (userPO.getId() == null) {
            userMapper.insert(userPO);
        } else {
            userMapper.updateById(userPO);
        }
        return toDomain(userPO);
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

    private User toDomain(UserPO userPO) {
        return User.builder()
            .id(userPO.getId())
            .wechatOpenId(userPO.getWechatOpenId())
            .wechatUnionId(userPO.getWechatUnionId())
            .nickname(userPO.getNickname())
            .avatarUrl(userPO.getAvatarUrl())
            .mobile(userPO.getMobile())
            .status(userPO.getStatus())
            .createdAt(userPO.getCreatedAt())
            .updatedAt(userPO.getUpdatedAt())
            .build();
    }

    private UserPO toPO(User user) {
        UserPO userPO = new UserPO();
        userPO.setId(user.getId());
        userPO.setWechatOpenId(user.getWechatOpenId());
        userPO.setWechatUnionId(user.getWechatUnionId());
        userPO.setNickname(user.getNickname());
        userPO.setAvatarUrl(user.getAvatarUrl());
        userPO.setMobile(user.getMobile());
        userPO.setStatus(user.getStatus());
        userPO.setCreatedAt(user.getCreatedAt());
        userPO.setUpdatedAt(user.getUpdatedAt());
        return userPO;
    }
}
