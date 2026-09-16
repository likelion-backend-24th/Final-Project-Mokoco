package com.team2.postservice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team2.postservice.ai.controller.AiController;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(assignableTypes=AiController.class)
public class AiResponseAdvice implements ResponseBodyAdvice<JsonNode> {

    @Override public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return JsonNode.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override public JsonNode beforeBodyWrite(JsonNode body, @NonNull MethodParameter returnType, @NonNull MediaType contentType,
                                              @NonNull Class<? extends HttpMessageConverter<?>> converterType, @NonNull ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().setCacheControl(CacheControl.noStore());

        if (body instanceof ObjectNode object) {
            object.put("requestId",AiRequestTrace.requestId());
        }
        return body;
    }
}
