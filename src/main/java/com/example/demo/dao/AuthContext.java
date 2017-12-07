package com.example.demo.dao;

/**
 * Created by zhangxs on 2016/9/22.
 */
public class AuthContext {
    private static ThreadLocal<String> LOCAL_USER_ID = new InheritableThreadLocal();

    public static void setUserId(String userId) {
        LOCAL_USER_ID.set(userId);
    }

    public static String getUserId() {
        return LOCAL_USER_ID.get();
    }

}
