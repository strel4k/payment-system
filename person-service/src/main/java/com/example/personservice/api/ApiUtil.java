package com.example.personservice.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.IOException;


public final class ApiUtil {

    private ApiUtil() {
    }

    public static void setExampleResponse(NativeWebRequest request, String contentType, String example) {
        if (request == null) return;

        HttpServletResponse response = request.getNativeResponse(HttpServletResponse.class);
        if (response == null) return;

        try {
            response.setCharacterEncoding("utf-8");
            response.setContentType(contentType);
            response.getWriter().write(example);
        } catch (IOException ignored) {
        }
    }
}