package com.example.studio.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

import com.example.studio.common.ApiException;
import com.example.studio.common.ErrorCode;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "studio.userId";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Cookie cookie = WebUtils.getCookie(request, AuthController.COOKIE_NAME);
        Long userId = authService.resolveUserId(cookie == null ? null : cookie.getValue());
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
