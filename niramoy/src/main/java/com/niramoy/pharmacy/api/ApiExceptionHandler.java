package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** মোবাইল API-র সব ত্রুটি { "message": "..." } আকারে ফেরত দেয় */
@RestControllerAdvice(basePackages = "com.niramoy.pharmacy.api")
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> business(BusinessException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        HttpStatusCode code = e.getStatusCode();
        String msg = e.getReason();
        if (msg == null || msg.isBlank()) msg = code.value() == 404 ? "তথ্য পাওয়া যায়নি।" : "অনুরোধ সম্পন্ন করা যায়নি।";
        return ResponseEntity.status(code).body(Map.of("message", msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().stream().findFirst()
                .map(err -> err.getDefaultMessage()).orElse("অনুরোধের তথ্য সঠিক নয়।");
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "অনুরোধের তথ্য সঠিক নয়।");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception e) {
        log.error("API ত্রুটি", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "সার্ভারে সমস্যা হয়েছে। পরে আবার চেষ্টা করুন।");
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of("message", m));
    }
}
