package com.spvermicelli.tripledger.shared.common.context;

public final class UserContextHolder {

    private static final ThreadLocal<LoginUser> USER_CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(LoginUser loginUser) {
        USER_CONTEXT.set(loginUser);
    }

    public static LoginUser get() {
        return USER_CONTEXT.get();
    }

    public static Long getUserId() {
        LoginUser loginUser = USER_CONTEXT.get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static String getTokenType() {
        LoginUser loginUser = USER_CONTEXT.get();
        return loginUser == null ? null : loginUser.getTokenType();
    }

    public static void clear() {
        USER_CONTEXT.remove();
    }
}
