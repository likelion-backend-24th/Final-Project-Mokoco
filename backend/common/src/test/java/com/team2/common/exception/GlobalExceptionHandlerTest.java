package com.team2.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    @Test
    void returnsSharedErrorStatusAndBody() throws Exception {
        MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build().perform(get("/room"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(TestError.CHAT_ROOM_NOT_FOUND.getMessage()));
    }

    @RestController
    static class TestController {
        @GetMapping("/room")
        void room() {
            throw new CustomException(TestError.CHAT_ROOM_NOT_FOUND);
        }
    }

    enum TestError implements ApiErrorCode {
        CHAT_ROOM_NOT_FOUND;
        public HttpStatus getHttpStatus() { return HttpStatus.NOT_FOUND; }
        public String getCode() { return name(); }
        public String getMessage() { return "채팅방을 찾을 수 없습니다."; }
    }
}
