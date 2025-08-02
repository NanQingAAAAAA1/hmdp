package com.hmdp.utils;

/**
 * redis相关常量类
 */
public class RedisConsts {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_TOKEN = "login:token:";
    public static final String LOGIN_USER_KEY = "login:token:"; // 验证码业务前缀
    public static final Long LOGIN_USER_TTL = 30L; // 有效期常量
}
