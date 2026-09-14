package com.team2.postservice.ai;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;

@RestControllerAdvice(assignableTypes=AiController.class)
public class AiErrors {
    @ExceptionHandler(AiException.class)
    ResponseEntity<Map<String,String>> ai(AiException error) {
        return ResponseEntity.status(error.status).cacheControl(CacheControl.noStore()).body(Map.of("code",error.code,"message",error.getMessage(),"requestId",AiRequestTrace.requestId()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    ResponseEntity<Map<String,String>> input(Exception error) { return ai(AiException.input("사진과 입력 항목을 확인해주세요.")); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String,String>> large(Exception error) { return ai(new AiException(HttpStatus.PAYLOAD_TOO_LARGE,"IMAGE_TOO_LARGE","사진 용량을 줄여주세요.")); }
}
