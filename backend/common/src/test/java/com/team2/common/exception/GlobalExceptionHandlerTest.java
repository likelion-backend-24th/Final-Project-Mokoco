package com.team2.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
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
                .andExpect(jsonPath("$.message").value(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage()));
    }

    @RestController
    static class TestController {
        @GetMapping("/room")
        void room() {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }
}
