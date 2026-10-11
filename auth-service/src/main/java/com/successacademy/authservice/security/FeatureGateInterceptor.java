package com.successacademy.authservice.security;

import com.successacademy.authservice.service.config.FeatureEntitlementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class FeatureGateInterceptor implements HandlerInterceptor {

    private final FeatureEntitlementService featureService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check method level annotation
        RequireFeature methodAnnotation = handlerMethod.getMethodAnnotation(RequireFeature.class);
        if (methodAnnotation != null) {
            featureService.requireFeature(methodAnnotation.value());
            return true;
        }

        // Check class level annotation
        RequireFeature classAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireFeature.class);
        if (classAnnotation != null) {
            featureService.requireFeature(classAnnotation.value());
            return true;
        }

        return true;
    }
}
