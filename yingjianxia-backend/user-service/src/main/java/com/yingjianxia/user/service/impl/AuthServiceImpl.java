package com.yingjianxia.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.constants.RedisKeyConstants;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ResultCode;
import com.yingjianxia.common.core.utils.JwtUtils;
import com.yingjianxia.user.constants.UserConstants;
import com.yingjianxia.user.dto.auth.*;
import com.yingjianxia.user.entity.Shop;
import com.yingjianxia.user.entity.User;
import com.yingjianxia.user.entity.UserAuth;
import com.yingjianxia.user.entity.UserRole;
import com.yingjianxia.user.enums.UserErrorCode;
import com.yingjianxia.user.mapper.ShopMapper;
import com.yingjianxia.user.mapper.UserAuthMapper;
import com.yingjianxia.user.mapper.UserMapper;
import com.yingjianxia.user.mapper.UserRoleMapper;
import com.yingjianxia.user.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RedissonClient redisson;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final ShopMapper shopMapper;
    private final UserRoleMapper userRoleMapper;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /* ============================================================
     *  发送短信验证码
     * ============================================================ */

    @Override
    public void sendSmsCode(SendSmsCodeReq req) {
        String phone = req.getPhone();
        String scene = req.getScene();

        // 1. 场景校验
        validateScene(scene);

        // 2. 频率限制（60秒内同号不重复发送）
        String freqKey = String.format(RedisKeyConstants.SMS_FREQ_LIMIT, phone);
        RBucket<Object> freqBucket = redisson.getBucket(freqKey);
        if (freqBucket.isExists()) {
            throw new BusinessException(UserErrorCode.SMS_CODE_SEND_FREQUENT);
        }

        // 3. 生成 6 位随机码
        String code = RandomUtil.randomNumbers(6);

        // 4. 场景为注册时：校验手机号是否已注册
        if (UserConstants.SMS_SCENE_REGISTER.equals(scene)) {
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (exists != null && exists > 0) {
                throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
            }
        }
        // 场景为登录/找回密码时：必须已注册
        if (UserConstants.SMS_SCENE_LOGIN.equals(scene) || UserConstants.SMS_SCENE_RESET_PWD.equals(scene)) {
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (exists == null || exists == 0) {
                throw new BusinessException(ResultCode.PHONE_NOT_REGISTERED);
            }
        }

        // 5. 存入 Redis（TTL 5 分钟）
        String codeKey = String.format(RedisKeyConstants.SMS_CODE, phone, scene);
        RBucket<String> codeBucket = redisson.getBucket(codeKey, StringCodec.INSTANCE);
        codeBucket.set(code, UserConstants.SMS_CODE_TTL_SEC, TimeUnit.SECONDS);

        // 6. 频率锁 60 秒
        freqBucket.set("1", UserConstants.SMS_FREQ_LIMIT_SEC, TimeUnit.SECONDS);

        // TODO: 调用真实短信服务商（阿里云/腾讯云）发送短信，此处仅打日志（开发/测试环境可前端从Redis取码）
        log.info("【短信验证码】手机号={}, 场景={}, 验证码={} (开发期写日志，生产调用SMS网关)", phone, scene, code);
    }

    private void validateScene(String scene) {
        if (StrUtil.isBlank(scene)) throw new BusinessException(UserErrorCode.SMS_SCENE_NOT_SUPPORTED);
        switch (scene) {
            case UserConstants.SMS_SCENE_REGISTER:
            case UserConstants.SMS_SCENE_LOGIN:
            case UserConstants.SMS_SCENE_RESET_PWD:
            case UserConstants.SMS_SCENE_REAL_NAME:
            case UserConstants.SMS_SCENE_CHANGE_PHONE:
                return;
            default:
                throw new BusinessException(UserErrorCode.SMS_SCENE_NOT_SUPPORTED);
        }
    }

    /* ============================================================
     *  手机号注册
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResp phoneRegister(PhoneRegisterReq req) {
        String phone = req.getPhone();

        // 1. 参数：2次密码一致 & 强度
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(UserErrorCode.PASSWORD_NOT_MATCH);
        }
        validatePasswordStrength(req.getPassword());

        // 2. 手机号唯一性（数据库唯一约束兜底，先预查给友好提示）
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PHONE_ALREADY_REGISTERED);
        }

        // 3. 验证码校验
        verifySmsCode(phone, UserConstants.SMS_SCENE_REGISTER, req.getSmsCode());

        // 4. 构造用户
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StrUtil.blankToDefault(req.getNickname(), defaultNickname(phone)));
        user.setCreditScore(UserConstants.DEFAULT_CREDIT_SCORE);
        user.setTransactionCount(0);
        user.setMonthlyIncome(new java.math.BigDecimal("0.00"));
        user.setSellerVerified(false);
        user.setStatus(User.STATUS_NORMAL);
        userMapper.insert(user);

        log.info("【用户注册成功】userId={}, phone={}", user.getId(), phone);

        // 5. 返回登录态
        return buildLoginResp(user);
    }

    /* ============================================================
     *  密码登录
     * ============================================================ */

    @Override
    public LoginResp passwordLogin(PasswordLoginReq req) {
        String phone = req.getPhone();

        // 1. 账号锁定检查
        String lockKey = String.format(RedisKeyConstants.ACCOUNT_LOCK, phone);
        if (redisson.getBucket(lockKey).isExists()) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }

        // 2. 用户查询
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 3. 账号状态
        checkAccountStatus(user);

        // 4. 密码校验
        boolean matched = passwordEncoder.matches(req.getPassword(), user.getPasswordHash());
        if (!matched) {
            handleLoginFail(phone, user.getId());
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 5. 登录成功：清除失败计数
        clearLoginFailCount(phone, user.getId());

        return buildLoginResp(user);
    }

    /* ============================================================
     *  验证码登录
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResp smsCodeLogin(SmsCodeLoginReq req) {
        String phone = req.getPhone();

        // 1. 验证码校验（场景=login）
        verifySmsCode(phone, UserConstants.SMS_SCENE_LOGIN, req.getSmsCode());

        // 2. 查用户，不存在则自动注册（验证码登录=无密码注册）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setPasswordHash(passwordEncoder.encode(RandomUtil.randomString(16))); // 随机占位，用户可后续修改
            user.setNickname(defaultNickname(phone));
            user.setCreditScore(UserConstants.DEFAULT_CREDIT_SCORE);
            user.setTransactionCount(0);
            user.setMonthlyIncome(new java.math.BigDecimal("0.00"));
            user.setSellerVerified(false);
            user.setStatus(User.STATUS_NORMAL);
            userMapper.insert(user);
            log.info("【验证码登录自动注册】userId={}, phone={}", user.getId(), phone);
        } else {
            checkAccountStatus(user);
            clearLoginFailCount(phone, user.getId());
        }

        return buildLoginResp(user);
    }

    @Override
    public void resetPassword(String phone, String smsCode, String newPassword) {
        // 1. 验证码校验（场景=resetPwd）
        verifySmsCode(phone, UserConstants.SMS_SCENE_RESET_PWD, smsCode);
        // 2. 密码强度校验
        validatePasswordStrength(newPassword);
        // 3. 查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        // 4. 更新密码
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getUpdatedAt, java.time.LocalDateTime.now()));
        log.info("【重置密码】userId={}, phone={}", user.getId(), phone);
    }

    /* ============================================================
     *  第三方 OAuth 登录（Mock 外部接口）
     * ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResp oauthLogin(OAuthLoginReq req) {
        String provider = req.getProvider();

        // 1. provider 支持
        if (!UserAuth.PROVIDER_WEIXIN.equals(provider)
                && !UserAuth.PROVIDER_QQ.equals(provider)
                && !UserAuth.PROVIDER_ALIPAY.equals(provider)) {
            throw new BusinessException(UserErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        }

        // 2. 用 code 换取 openId（对接第三方 Mock）
        String openId = mockExchangeCodeForOpenId(provider, req.getCode());
        if (StrUtil.isBlank(openId)) {
            throw new BusinessException(UserErrorCode.OAUTH_CODE_INVALID);
        }

        // 3. 若已绑定，直接登录
        UserAuth userAuth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getProvider, provider)
                .eq(UserAuth::getOpenId, openId));
        if (userAuth != null) {
            User user = userMapper.selectById(userAuth.getUserId());
            if (user == null) {
                // 数据异常：绑定存在但用户不存在，解绑脏数据
                userAuthMapper.deleteById(userAuth.getId());
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "绑定账号异常，请重新授权");
            }
            checkAccountStatus(user);
            return buildLoginResp(user);
        }

        // 4. 未绑定 → 自动创建新用户（首次登录），并绑定第三方
        // 注：PRD 要求微信小程序登录如果无手机号，则前端需走绑定流程；这里以自动建号+待绑定表示
        User newUser = new User();
        newUser.setPasswordHash(passwordEncoder.encode(RandomUtil.randomString(16)));
        newUser.setNickname(provider + "_" + RandomUtil.randomString(6));
        newUser.setCreditScore(UserConstants.DEFAULT_CREDIT_SCORE);
        newUser.setTransactionCount(0);
        newUser.setMonthlyIncome(new java.math.BigDecimal("0.00"));
        newUser.setSellerVerified(false);
        newUser.setStatus(User.STATUS_NORMAL);
        userMapper.insert(newUser);

        UserAuth bind = new UserAuth();
        bind.setUserId(newUser.getId());
        bind.setProvider(provider);
        bind.setOpenId(openId);
        // 微信专用 unionId（mock 中留空）
        bind.setUnionId(null);
        userAuthMapper.insert(bind);

        log.info("【第三方登录首次绑定】userId={}, provider={}, openId={}", newUser.getId(), provider, openId);
        return buildLoginResp(newUser);
    }

    /* ============================================================
     *  刷新 Token
     * ============================================================ */

    @Override
    public LoginResp refreshToken(RefreshTokenReq req) {
        String refreshToken = req.getRefreshToken();

        // 1. 必须是合法 Refresh Token
        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException(UserErrorCode.TOKEN_REFRESH_REQUIRED);
        }
        if (jwtUtils.isExpired(refreshToken)) {
            throw new BusinessException(UserErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 2. 黑名单检查
        Claims claims = jwtUtils.parseToken(refreshToken);
        String jti = claims.getId();
        if (isTokenBlacklisted(jti)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 3. 查用户
        Long userId = Long.valueOf(claims.getSubject());
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        checkAccountStatus(user);

        // 4. 将旧 Refresh Token 加入黑名单
        blacklistToken(refreshToken);

        // 5. 滚动签发新的双 Token
        return buildLoginResp(user);
    }

    /* ============================================================
     *  登出
     * ============================================================ */

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (StrUtil.isNotBlank(accessToken) && !jwtUtils.isExpired(accessToken)) {
            blacklistToken(accessToken);
            // 同时清除设备活跃记录
            try {
                Claims claims = jwtUtils.parseToken(accessToken);
                String jti = claims.getId();
                Long userId = Long.valueOf(claims.getSubject());
                String key = String.format(RedisKeyConstants.USER_TOKEN_ACTIVE, userId);
                redisson.getMap(key, StringCodec.INSTANCE).remove(jti);
            } catch (Exception ignore) {
            }
        }
        if (StrUtil.isNotBlank(refreshToken) && !jwtUtils.isExpired(refreshToken)) {
            blacklistToken(refreshToken);
        }
        log.info("【登出】Token已加入黑名单");
    }

    /* ============================================================
     *  内部工具
     * ============================================================ */

    /**
     * 验证码校验，校验通过立即删除（一次性）
     */
    private void verifySmsCode(String phone, String scene, String userCode) {
        String key = String.format(RedisKeyConstants.SMS_CODE, phone, scene);
        RBucket<String> bucket = redisson.getBucket(key, StringCodec.INSTANCE);
        String stored = bucket.get();
        if (StrUtil.isBlank(stored) || !stored.equalsIgnoreCase(userCode)) {
            throw new BusinessException(UserErrorCode.SMS_CODE_INVALID);
        }
        bucket.delete(); // 验证成功后立刻失效
    }

    /**
     * 密码强度：至少含一个大写、一个小写、一个数字，长度8-20
     */
    private void validatePasswordStrength(String password) {
        if (StrUtil.isBlank(password) || !password.matches(UserConstants.PASSWORD_PATTERN)) {
            throw new BusinessException(UserErrorCode.PASSWORD_STRENGTH_WEAK);
        }
    }

    /**
     * 检查账号状态（冻结/封禁）
     */
    private void checkAccountStatus(User user) {
        if (User.STATUS_FROZEN == user.getStatus() || User.STATUS_BANNED == user.getStatus()) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
    }

    /**
     * 密码错误计数 + 超过阈值锁定
     */
    private void handleLoginFail(String phone, Long userId) {
        String failKey = String.format(RedisKeyConstants.LOGIN_FAIL_COUNT, phone);
        RBucket<Integer> bucket = redisson.getBucket(failKey);
        Integer count = bucket.get();
        count = (count == null) ? 1 : count + 1;
        bucket.set(count, UserConstants.LOGIN_FAIL_TTL_SEC, TimeUnit.SECONDS);

        if (count >= UserConstants.LOGIN_FAIL_THRESHOLD) {
            String lockKey = String.format(RedisKeyConstants.ACCOUNT_LOCK, userId != null ? userId : phone);
            redisson.getBucket(lockKey).set("1", UserConstants.LOGIN_FAIL_TTL_SEC, TimeUnit.SECONDS);
            log.warn("【账号锁定】phone={}, userId={}, 连续错误{}次", phone, userId, count);
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }
    }

    private void clearLoginFailCount(String phone, Long userId) {
        redisson.getBucket(String.format(RedisKeyConstants.LOGIN_FAIL_COUNT, phone)).delete();
        if (userId != null) {
            redisson.getBucket(String.format(RedisKeyConstants.ACCOUNT_LOCK, userId)).delete();
        }
    }

    /**
     * Token 放入黑名单（TTL = 剩余有效期或默认2h/7d）
     */
    private void blacklistToken(String token) {
        try {
            Claims c = jwtUtils.parseToken(token);
            String jti = c.getId();
            long remainMs = c.getExpiration().getTime() - System.currentTimeMillis();
            if (remainMs > 0) {
                String key = String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti);
                redisson.getBucket(key).set("1", remainMs, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignore) {
            // 解析失败直接忽略
        }
    }

    /**
     * 是否在黑名单
     */
    public boolean isTokenBlacklisted(String jti) {
        return redisson.getBucket(String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti)).isExists();
    }

    /**
     * 组装登录响应（双 Token + 用户信息）
     * 角色从 user_roles 表查询，无角色记录时默认 BUYER
     */
    private LoginResp buildLoginResp(User user) {
        // 从 user_roles 表查询用户角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<String> roles = new ArrayList<>();
        if (userRoles != null && !userRoles.isEmpty()) {
            for (UserRole ur : userRoles) {
                if (StrUtil.isNotBlank(ur.getRole())) {
                    roles.add(ur.getRole());
                }
            }
        }
        // 兜底：无角色记录时默认 BUYER
        if (roles.isEmpty()) {
            roles.add(UserConstants.ROLE_BUYER);
        }
        // 若已开店但无 SELLER 角色，附加 SELLER
        if (!roles.contains(UserConstants.ROLE_SELLER)) {
            Long shopCount = shopMapper.selectCount(new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getSellerId, user.getId()));
            if (shopCount != null && shopCount > 0) {
                roles.add(UserConstants.ROLE_SELLER);
            }
        }
        String accessToken = jwtUtils.generateAccessToken(user.getId(), roles, maskPhone(user.getPhone()));
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return LoginResp.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .accessToken(accessToken)
                .expiresIn(jwtUtils.getRemainingExpireMs(accessToken))
                .refreshToken(refreshToken)
                .build();
    }

    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String defaultNickname(String phone) {
        return "用户" + phone.substring(phone.length() - 4);
    }

    /**
     * Mock 第三方：用 code 换 openId — 生产环境对接各平台 OAuth2 接口
     */
    private String mockExchangeCodeForOpenId(String provider, String code) {
        // 生产对接示例：
        // 微信：https://api.weixin.qq.com/sns/oauth2/access_token?appid=...&code=code
        // QQ：https://graph.qq.com/oauth2.0/token
        // 支付宝：alipay.system.oauth.token
        // 这里直接以 code 哈希作为 mock openId
        if (StrUtil.isBlank(code)) return null;
        return provider + "_" + cn.hutool.crypto.SecureUtil.md5(code).substring(0, 16);
    }

    /**
     * 公共：根据 JTI 检查黑名单（供网关过滤器远程调用/Feign 用）
     */
    public boolean isJtiBlacklisted(String jti) {
        return redisson.getBucket(String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti)).isExists();
    }

    /* ============================================================
     *  登录设备管理
     * ============================================================ */

    @Override
    public void recordDeviceSession(Long userId, String accessToken, DeviceInfo deviceInfo) {
        try {
            Claims claims = jwtUtils.parseToken(accessToken);
            String jti = claims.getId();
            long expireMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (expireMs <= 0) return;

            String key = String.format(RedisKeyConstants.USER_TOKEN_ACTIVE, userId);
            RMap<String, String> map = redisson.getMap(key, StringCodec.INSTANCE);

            DeviceSession entry = new DeviceSession();
            entry.jti = jti;
            entry.userAgent = deviceInfo != null ? deviceInfo.getUserAgent() : "";
            entry.ip = deviceInfo != null ? deviceInfo.getIp() : "";
            entry.loginAt = LocalDateTime.now();
            entry.expireAt = claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            map.put(jti, objectMapper.writeValueAsString(entry));
            // 续期 Hash 整体 TTL 为当前 Token 剩余有效期
            map.expire(expireMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("【设备会话记录失败】userId={}, err={}", userId, e.getMessage());
        }
    }

    @Override
    public List<LoginDeviceResp> listDevices(Long userId, String currentJti) {
        String key = String.format(RedisKeyConstants.USER_TOKEN_ACTIVE, userId);
        RMap<String, String> map = redisson.getMap(key, StringCodec.INSTANCE);
        if (!map.isExists()) return Collections.emptyList();

        LocalDateTime now = LocalDateTime.now();
        List<LoginDeviceResp> result = new ArrayList<>();
        List<String> staleJtis = new ArrayList<>();

        for (Map.Entry<String, String> e : map.entrySet()) {
            String jti = e.getKey();
            try {
                DeviceSession ds = objectMapper.readValue(e.getValue(), DeviceSession.class);
                // 过滤已自然过期的记录（延迟清理）
                if (ds.expireAt != null && ds.expireAt.isBefore(now)) {
                    staleJtis.add(jti);
                    continue;
                }
                // 过滤已被加入黑名单的（踢出/登出后残留）
                if (isTokenBlacklisted(jti)) {
                    staleJtis.add(jti);
                    continue;
                }
                result.add(toDeviceResp(ds, jti, currentJti));
            } catch (Exception ex) {
                staleJtis.add(jti);
            }
        }

        // 清理过期/无效条目
        if (!staleJtis.isEmpty()) {
            staleJtis.forEach(map::remove);
        }
        // 按登录时间倒序
        result.sort((a, b) -> {
            if (a.getLoginAt() == null) return 1;
            if (b.getLoginAt() == null) return -1;
            return b.getLoginAt().compareTo(a.getLoginAt());
        });
        return result;
    }

    @Override
    public void kickDevice(Long userId, String jti) {
        String key = String.format(RedisKeyConstants.USER_TOKEN_ACTIVE, userId);
        RMap<String, String> map = redisson.getMap(key, StringCodec.INSTANCE);
        String json = map.get(jti);
        if (json == null) {
            throw new BusinessException(UserErrorCode.DEVICE_NOT_FOUND);
        }
        // 删除活跃记录
        map.remove(jti);
        // 将该 JTI 加入黑名单（TTL 取 Token 剩余有效期）
        try {
            DeviceSession ds = objectMapper.readValue(json, DeviceSession.class);
            long remainMs = ds.expireAt != null
                    ? java.time.Duration.between(LocalDateTime.now(), ds.expireAt).toMillis()
                    : jwtUtils.getAccessExpire();
            if (remainMs > 0) {
                redisson.getBucket(String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti))
                        .set("1", remainMs, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ex) {
            // 解析失败则使用默认 TTL
            redisson.getBucket(String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti))
                    .set("1", jwtUtils.getAccessExpire(), TimeUnit.MILLISECONDS);
        }
        log.info("【踢出设备】userId={}, jti={}", userId, jti);
    }

    @Override
    public void kickAllOtherDevices(Long userId, String currentJti) {
        String key = String.format(RedisKeyConstants.USER_TOKEN_ACTIVE, userId);
        RMap<String, String> map = redisson.getMap(key, StringCodec.INSTANCE);
        if (!map.isExists()) return;

        LocalDateTime now = LocalDateTime.now();
        // 先收集所有待踢出的 jti -> json 映射（不能边遍历边删除）
        List<Map.Entry<String, String>> toKick = new ArrayList<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!e.getKey().equals(currentJti)) {
                toKick.add(e);
            }
        }

        for (Map.Entry<String, String> entry : toKick) {
            String jti = entry.getKey();
            String json = entry.getValue();
            // 删除活跃记录
            map.remove(jti);
            // 加入黑名单
            try {
                long remainMs = jwtUtils.getAccessExpire();
                if (json != null) {
                    DeviceSession ds = objectMapper.readValue(json, DeviceSession.class);
                    if (ds.expireAt != null) {
                        remainMs = Math.max(0, java.time.Duration.between(now, ds.expireAt).toMillis());
                    }
                }
                if (remainMs > 0) {
                    redisson.getBucket(String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti))
                            .set("1", remainMs, TimeUnit.MILLISECONDS);
                }
            } catch (Exception ex) {
                redisson.getBucket(String.format(RedisKeyConstants.TOKEN_BLACKLIST, jti))
                        .set("1", jwtUtils.getAccessExpire(), TimeUnit.MILLISECONDS);
            }
        }
        log.info("【退出所有其他设备】userId={}, 踢出{}台", userId, toKick.size());
    }

    /**
     * 设备会话内部存储结构（Redis JSON 值）
     */
    @lombok.Data
    private static class DeviceSession {
        private String jti;
        private String userAgent;
        private String ip;
        private LocalDateTime loginAt;
        private LocalDateTime expireAt;
    }

    /**
     * 将内部存储转为响应 DTO（含 UA 解析）
     */
    private LoginDeviceResp toDeviceResp(DeviceSession ds, String jti, String currentJti) {
        LoginDeviceResp resp = new LoginDeviceResp();
        resp.setJti(jti);
        resp.setIp(ds.ip);
        resp.setLoginAt(ds.loginAt);
        resp.setLastActiveAt(ds.loginAt); // 暂以登录时间为最后活跃时间
        resp.setCurrent(jti.equals(currentJti));
        // 简单解析 User-Agent
        String ua = ds.userAgent != null ? ds.userAgent : "";
        resp.setOs(parseOs(ua));
        resp.setBrowser(parseBrowser(ua));
        resp.setDeviceName(buildDeviceName(resp.getOs(), resp.getBrowser()));
        return resp;
    }

    private String parseOs(String ua) {
        if (ua.contains("Windows NT")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Linux")) return "Linux";
        return "未知";
    }

    private String parseBrowser(String ua) {
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Safari";
        if (ua.contains("MicroMessenger")) return "微信内置浏览器";
        return "未知";
    }

    private String buildDeviceName(String os, String browser) {
        if ("未知".equals(os) && "未知".equals(browser)) return "未知设备";
        return browser + " on " + os;
    }
}
