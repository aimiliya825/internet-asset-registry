package com.asset.registry.config;

/**
 * 请求级用户上下文，避免每个方法透传用户对象。
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        CurrentUser u = HOLDER.get();
        return u == null ? CurrentUser.system() : u;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
