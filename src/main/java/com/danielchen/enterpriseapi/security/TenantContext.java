package com.danielchen.enterpriseapi.security;

/**
 * Holds the authenticated user for the current request thread.
 * Set by JwtAuthFilter; read by services that need org-scoped queries (M3+).
 */
public final class TenantContext {

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    public static AuthenticatedUser get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
