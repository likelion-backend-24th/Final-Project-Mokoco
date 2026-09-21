package com.team2.chatservice.chatMessage;

import com.team2.common.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 로그인 인증 자체는 SecurityConfig의 TokenAuthenticationFilter가 담당하므로(전체 스프링 컨텍스트
// 없이는 standalone MockMvc로 재현할 수 없음), 여기선 인증된 LoginUser가 항상 주입된다고 가정하고
// 컨트롤러 자신의 로직(Range 헤더 partial content 처리)만 검증한다.
class ChatAttachmentRangeTest {
    @TempDir Path directory;
    @Test void returnsPartialVideoContent() throws Exception {
        Path file = Files.write(directory.resolve("video"), new byte[]{0,1,2,3,4,5,6,7});
        var service = mock(ChatAttachmentService.class);
        when(service.download(1L, 3L, 2L)).thenReturn(ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4")).body(new FileSystemResource(file)));
        var mvc = MockMvcBuilders.standaloneSetup(new ChatAttachmentController(service, mock(SimpMessagingTemplate.class)))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(MethodParameter parameter) { return parameter.getParameterType() == LoginUser.class; }
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) { return new LoginUser(2L, "u"); }
                })
                .build();
        mvc.perform(get("/api/chat-rooms/1/attachments/3").header("Range", "bytes=2-4"))
                .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 2-4/8"))
                .andExpect(content().bytes(new byte[]{2,3,4}));
        verify(service).download(1L, 3L, 2L);
    }
}
