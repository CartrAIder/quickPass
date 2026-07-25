package com.mart.quickpass.cart.controller;

import com.mart.quickpass.cart.dto.SseTicketResponse;
import com.mart.quickpass.cart.sse.CartSseService;
import com.mart.quickpass.cart.sse.SseTicketService;
import com.mart.quickpass.global.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartSseController {

    private final CartSseService cartSseService;
    private final SseTicketService sseTicketService;


    // 구독용 티켓 발급 메서드
    @PostMapping("/sse-ticket")
    public ResponseEntity<SseTicketResponse> issueTicket(@AuthenticationPrincipal Long userId) {
        String ticket = sseTicketService.issue(userId);
        return ResponseEntity.ok(new SseTicketResponse(ticket, sseTicketService.ttlSeconds()));
    }

    // 티켓 검증 + 연동 메서드
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam("ticket") String ticket) {
        Long userId = sseTicketService.consume(ticket)
                .orElseThrow(() -> new InvalidTokenException("유효하지 않거나 만료된 SSE 티켓입니다."));
        return cartSseService.subscribe(userId);
    }
}
