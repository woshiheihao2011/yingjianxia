package com.yingjianxia.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yingjianxia.common.core.constants.RedisKeyConstants;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ResultCode;
import com.yingjianxia.user.constants.UserConstants;
import com.yingjianxia.user.dto.profile.*;
import com.yingjianxia.user.entity.Address;
import com.yingjianxia.user.entity.Shop;
import com.yingjianxia.user.entity.User;
import com.yingjianxia.user.entity.UserRealName;
import com.yingjianxia.user.enums.UserErrorCode;
import com.yingjianxia.user.mapper.AddressMapper;
import com.yingjianxia.user.mapper.ShopMapper;
import com.yingjianxia.user.mapper.UserMapper;
import com.yingjianxia.user.mapper.UserRealNameMapper;
import com.yingjianxia.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户资料服务实现
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final UserRealNameMapper realNameMapper;
    private final AddressMapper addressMapper;
    private final ShopMapper shopMapper;
    private final RedissonClient redisson;

    /* ============================================================
     *  个人中心资料
     * ============================================================ */

    @Override
    public UserProfileResp getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);

        // 实名状态
        Integer realNameStatus = getRealNameStatus(userId);

        // 已开通的店铺ID
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getSellerId, userId));

        return buildProfileResp(user, realNameStatus, shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileReq req) {
        if (req == null) return;
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId);
        boolean needUpdate = false;

        if (StrUtil.isNotBlank(req.getNickname())) {
            if (req.getNickname().length() > 50) throw new BusinessException(UserErrorCode.NICKNAME_TOO_LONG);
            // 重复性检查：如果和当前不同则检查
            Long dup = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getNickname, req.getNickname())
                    .ne(User::getId, userId));
            if (dup != null && dup > 0) throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATE);
            uw.set(User::getNickname, req.getNickname());
            needUpdate = true;
        }
        if (req.getAvatarUrl() != null) {
            if (req.getAvatarUrl().length() > 512) throw new BusinessException(UserErrorCode.AVATAR_UPLOAD_FAIL);
            uw.set(User::getAvatarUrl, req.getAvatarUrl());
            needUpdate = true;
        }
        if (req.getBio() != null) {
            if (req.getBio().length() > 200) throw new BusinessException(UserErrorCode.BIO_TOO_LONG);
            uw.set(User::getBio, req.getBio());
            needUpdate = true;
        }
        if (req.getEmail() != null) {
            if (req.getEmail().length() > 128) throw new BusinessException("邮箱长度不能超过128个字符");
            // 邮箱重复性检查
            Long dupEmail = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, req.getEmail())
                    .ne(User::getId, userId));
            if (dupEmail != null && dupEmail > 0) throw new BusinessException(UserErrorCode.EMAIL_DUPLICATE);
            uw.set(User::getEmail, req.getEmail());
            needUpdate = true;
        }
        if (req.getGender() != null) {
            uw.set(User::getGender, req.getGender());
            needUpdate = true;
        }
        if (req.getBirthday() != null) {
            try {
                uw.set(User::getBirthday, java.time.LocalDate.parse(req.getBirthday()));
            } catch (Exception e) {
                throw new BusinessException("生日格式错误，应为 yyyy-MM-dd");
            }
            needUpdate = true;
        }
        if (req.getRegion() != null) {
            if (req.getRegion().length() > 100) throw new BusinessException("地区长度不能超过100个字符");
            uw.set(User::getRegion, req.getRegion());
            needUpdate = true;
        }
        if (needUpdate) {
            uw.set(User::getUpdatedAt, LocalDateTime.now());
            userMapper.update(null, uw);
        }
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.OLD_PASSWORD_ERROR);
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, encoder.encode(newPassword))
                .set(User::getUpdatedAt, LocalDateTime.now()));
        log.info("【修改密码】userId={}", userId);
    }

    /* ============================================================
     *  实名认证
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRealName(Long userId, RealNameSubmitReq req) {
        // 1. 验证码
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        if (StrUtil.isBlank(user.getPhone())) {
            throw new BusinessException("当前账号未绑定手机号，请先绑定");
        }
        // 短信验证码可选：前端如果传了 smsCode 则校验，不传则跳过
        if (StrUtil.isNotBlank(req.getSmsCode())) {
            verifySmsCode(user.getPhone(), UserConstants.SMS_SCENE_REAL_NAME, req.getSmsCode());
        }

        // 2. 一人一认证，不可重复提交
        Long dupUser = realNameMapper.selectCount(new LambdaQueryWrapper<UserRealName>()
                .eq(UserRealName::getUserId, userId));
        if (dupUser != null && dupUser > 0) {
            throw new BusinessException(UserErrorCode.REAL_NAME_DUPLICATE);
        }

        // 3. 身份证号不可被他人占用
        String encryptedIdCard = aesEncrypt(req.getIdCardNo());
        Long dupCard = realNameMapper.selectCount(new LambdaQueryWrapper<UserRealName>()
                .eq(UserRealName::getIdCardNo, encryptedIdCard));
        if (dupCard != null && dupCard > 0) {
            throw new BusinessException(UserErrorCode.REAL_NAME_ID_CARD_USED);
        }

        // 4. 对接公安三要素核验（Mock：姓名+身份证匹配）
        boolean passed = mockPoliceVerify(req.getRealName(), req.getIdCardNo());

        // 5. 写库
        UserRealName rn = new UserRealName();
        rn.setUserId(userId);
        rn.setRealName(aesEncrypt(req.getRealName())); // 姓名也加密
        rn.setIdCardNo(encryptedIdCard);
        rn.setVerificationStatus(passed ? UserRealName.STATUS_PASSED : UserRealName.STATUS_REJECTED);
        rn.setVerifiedAt(passed ? LocalDateTime.now() : null);
        realNameMapper.insert(rn);

        if (!passed) {
            throw new BusinessException(UserErrorCode.REAL_NAME_VERIFY_FAILED);
        }

        log.info("【实名认证成功】userId={}", userId);
    }

    @Override
    public Integer getRealNameStatus(Long userId) {
        UserRealName rn = realNameMapper.selectOne(new LambdaQueryWrapper<UserRealName>()
                .eq(UserRealName::getUserId, userId));
        if (rn == null) return UserRealName.STATUS_PENDING; // 未申请过显示"待认证"
        return rn.getVerificationStatus();
    }

    /* ============================================================
     *  收货地址 CRUD
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addAddress(Long userId, AddressReq req) {
        // 1. 上限
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
        if (count != null && count >= UserConstants.MAX_ADDRESS_COUNT) {
            throw new BusinessException(UserErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        // 2. 设置默认：若为第一条，自动改为默认
        boolean isDefault = Boolean.TRUE.equals(req.getIsDefault()) || (count == null || count == 0);
        if (isDefault) {
            // 清除其他默认
            addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, true)
                    .set(Address::getIsDefault, false));
        }

        Address addr = new Address();
        addr.setUserId(userId);
        addr.setReceiverName(req.getReceiverName());
        addr.setPhone(req.getPhone());
        addr.setProvince(req.getProvince());
        addr.setCity(req.getCity());
        addr.setDistrict(req.getDistrict());
        addr.setDetail(req.getDetail());
        addr.setIsDefault(isDefault);
        addressMapper.insert(addr);
        return addr.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, Long addressId, AddressReq req) {
        Address addr = assertAddressOwner(userId, addressId);

        // 默认逻辑
        if (Boolean.TRUE.equals(req.getIsDefault()) && !Boolean.TRUE.equals(addr.getIsDefault())) {
            addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, true)
                    .ne(Address::getId, addressId)
                    .set(Address::getIsDefault, false));
        }

        LambdaUpdateWrapper<Address> uw = new LambdaUpdateWrapper<>();
        uw.eq(Address::getId, addressId)
                .set(Address::getReceiverName, req.getReceiverName())
                .set(Address::getPhone, req.getPhone())
                .set(Address::getProvince, req.getProvince())
                .set(Address::getCity, req.getCity())
                .set(Address::getDistrict, req.getDistrict())
                .set(Address::getDetail, req.getDetail())
                .set(Address::getIsDefault, req.getIsDefault())
                .set(Address::getUpdatedAt, LocalDateTime.now());
        addressMapper.update(null, uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long userId, Long addressId) {
        Address addr = assertAddressOwner(userId, addressId);
        // 默认地址不可直接删除
        if (Boolean.TRUE.equals(addr.getIsDefault())) {
            Long remain = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .ne(Address::getId, addressId));
            if (remain != null && remain > 0) {
                throw new BusinessException(UserErrorCode.DEFAULT_ADDRESS_CANNOT_DELETE);
            }
            // 只剩默认地址，允许删除（最后一个）
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long addressId) {
        assertAddressOwner(userId, addressId);
        // 清除他人默认
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, true)
                .ne(Address::getId, addressId)
                .set(Address::getIsDefault, false));
        // 设置本地址默认
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, addressId)
                .set(Address::getIsDefault, true)
                .set(Address::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public List<AddressResp> listAddress(Long userId) {
        List<Address> list = addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByAsc(Address::getId));
        return list.stream().map(this::toAddrResp).collect(Collectors.toList());
    }

    @Override
    public AddressResp getAddress(Long userId, Long addressId) {
        Address a = assertAddressOwner(userId, addressId);
        return toAddrResp(a);
    }

    /* ============================================================
     *  信用分
     * ============================================================ */

    @Override
    public BigDecimal getCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        return user.getCreditScore() != null ? user.getCreditScore() : UserConstants.DEFAULT_CREDIT_SCORE;
    }

    /* ============================================================
     *  信用分定时刷新（每日 XXL-Job 调度）
     * ============================================================ */
    @Override
    @XxlJob("userCreditScoreRefreshJob")
    @Transactional(rollbackFor = Exception.class)
    public void refreshCreditScoreBatch() {
        log.info("【信用分刷新】任务开始");
        // 分页全量扫描用户
        long pageSize = 500;
        long lastId = 0L;
        int processed = 0;
        while (true) {
            List<User> page = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .gt(User::getId, lastId)
                    .orderByAsc(User::getId)
                    .last("LIMIT " + pageSize));
            if (page == null || page.isEmpty()) break;

            for (User user : page) {
                BigDecimal newScore = calcCreditScore(user);
                if (newScore.compareTo(user.getCreditScore()) != 0) {
                    userMapper.update(null, new LambdaUpdateWrapper<User>()
                            .eq(User::getId, user.getId())
                            .set(User::getCreditScore, newScore)
                            .set(User::getUpdatedAt, LocalDateTime.now()));
                }
                lastId = user.getId();
                processed++;
            }
        }
        log.info("【信用分刷新】任务结束，处理用户数={}", processed);
    }

    /* ============================================================
     *  内部工具
     * ============================================================ */

    private UserProfileResp buildProfileResp(User user, Integer realNameStatus, Shop shop) {
        UserProfileResp resp = new UserProfileResp();
        resp.setUserId(user.getId());
        resp.setPhone(maskPhone(user.getPhone()));
        resp.setEmail(user.getEmail());
        resp.setNickname(user.getNickname());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setBio(user.getBio());
        resp.setGender(user.getGender());
        resp.setBirthday(user.getBirthday() != null ? user.getBirthday().toString() : null);
        resp.setRegion(user.getRegion());
        resp.setCreditScore(user.getCreditScore());
        resp.setTransactionCount(user.getTransactionCount());
        resp.setMonthlyIncome(user.getMonthlyIncome());
        resp.setSellerVerified(user.getSellerVerified());
        resp.setRealNameStatus(realNameStatus);
        resp.setShopId(shop != null ? shop.getId() : null);
        resp.setStatus(user.getStatus());
        resp.setCreatedAt(user.getCreatedAt());
        return resp;
    }

    private AddressResp toAddrResp(Address a) {
        AddressResp r = new AddressResp();
        r.setId(a.getId());
        r.setReceiverName(a.getReceiverName());
        r.setPhone(maskPhone(a.getPhone()));
        r.setProvince(a.getProvince());
        r.setCity(a.getCity());
        r.setDistrict(a.getDistrict());
        r.setDetail(a.getDetail());
        String full = (a.getProvince() == null ? "" : a.getProvince())
                + (a.getCity() == null ? "" : a.getCity())
                + (a.getDistrict() == null ? "" : a.getDistrict())
                + (a.getDetail() == null ? "" : a.getDetail());
        r.setFullAddress(full);
        r.setIsDefault(a.getIsDefault());
        r.setUpdatedAt(a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt());
        return r;
    }

    private Address assertAddressOwner(Long userId, Long addressId) {
        Address a = addressMapper.selectById(addressId);
        if (a == null) throw new BusinessException(UserErrorCode.ADDRESS_NOT_FOUND);
        if (!a.getUserId().equals(userId)) throw new BusinessException(UserErrorCode.ADDRESS_NOT_BELONG_TO_USER);
        return a;
    }

    private void verifySmsCode(String phone, String scene, String userCode) {
        String key = String.format(RedisKeyConstants.SMS_CODE, phone, scene);
        RBucket<String> bucket = redisson.getBucket(key, StringCodec.INSTANCE);
        String stored = bucket.get();
        if (StrUtil.isBlank(stored) || !stored.equalsIgnoreCase(userCode)) {
            throw new BusinessException(UserErrorCode.SMS_CODE_INVALID);
        }
        bucket.delete();
    }

    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (StrUtil.isBlank(email)) return null;
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String head = email.substring(0, Math.min(2, at));
        return head + "***" + email.substring(at);
    }

    /**
     * 信用分算法（0.0-5.0）
     * <p>
     * 基础分 4.0
     * + 成交数 × 0.01（上限 +0.5）
     * - 差评率 × 2.0（每 10% 差评扣 0.2，上限 -3.0）
     * - 纠纷次数 × 0.2（每笔纠纷扣 0.2，上限 -2.0）
     * - 违约次数 × 0.5（每笔违约扣 0.5，无上限）
     * 最终 clamp 在 [0.0, 5.0]
     * <p>
     * 注：差评/纠纷/违约数据目前占位（订单/售后/客服域同步完成后，通过 MQ 更新 users 冗余字段或直接 JOIN 查），
     * 当前实现基于 transaction_count 估算，后续可接评价/售后域 Feign。
     */
    private BigDecimal calcCreditScore(User user) {
        BigDecimal score = new BigDecimal("4.0");
        // 成交加分
        int tx = user.getTransactionCount() == null ? 0 : user.getTransactionCount();
        BigDecimal txBonus = BigDecimal.valueOf(Math.min(tx * 0.01d, 0.5d));
        score = score.add(txBonus);
        // 新用户首笔送满 5.0
        if (tx == 0) {
            score = UserConstants.DEFAULT_CREDIT_SCORE;
        }
        // 区间约束
        if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
        if (score.compareTo(new BigDecimal("5.0")) > 0) score = new BigDecimal("5.0");
        return score.setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 姓名/身份证加密（简化：AES Mock — 真实环境注入密钥）
     */
    private String aesEncrypt(String plain) {
        // 生产使用 AES/GCM/NoPadding + KMS 密钥
        if (StrUtil.isBlank(plain)) return plain;
        return "ENC:" + cn.hutool.crypto.SecureUtil.aes(
                "yingjianxia-key-1234567890123456".getBytes()).encryptHex(plain);
    }

    /**
     * Mock 公安三要素校验 — 生产对接公安一所认证/CTID/第三方认证服务
     */
    private boolean mockPoliceVerify(String name, String idCard) {
        // 规则：姓名非空 + 身份证18位通过校验位 即视为通过（生产调用外部接口）
        if (StrUtil.isBlank(name) || StrUtil.isBlank(idCard)) return false;
        if (!idCard.matches("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$")) {
            return false;
        }
        // ID 校验位校验
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] codes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * weights[i];
        }
        char expected = codes[sum % 11];
        char actual = Character.toUpperCase(idCard.charAt(17));
        return expected == actual;
    }
}
