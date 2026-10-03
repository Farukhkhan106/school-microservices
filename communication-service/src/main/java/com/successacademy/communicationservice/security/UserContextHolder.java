package com.successacademy.communicationservice.security;

public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static UserContext require() {
        UserContext ctx = CONTEXT.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw new SecurityException("Authenticated user context is required");
        }
        return ctx;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
