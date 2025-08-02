package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

/**
 * 静态ThreadLocal方法定义类
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    // 将user存进ThreadLocal
    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    // 将user从ThreadLocal取出
    public static UserDTO getUser(){
        return tl.get();
    }

    // 将user从ThreadLocal移除
    public static void removeUser(){
        tl.remove();
    }
}
