package com.example.order.controller;

import com.example.order.domain.Order;
import com.example.order.dto.OrderRequest;
import com.example.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "주문 생성, 조회, 상태 변경, 매출 통계")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> create(
            @RequestBody OrderRequest req,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.create(userId, req));
    }

    @GetMapping("/my")
    public List<Order> myOrders(@RequestHeader("X-User-Id") Long userId) {
        return orderService.getMyOrders(userId);
    }

    @GetMapping
    public ResponseEntity<List<Order>> allOrders(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderService.updateStatus(id, body.get("status")));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(
            @RequestParam String type,
            @RequestParam String value,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(orderService.getStats(type, value));
    }
}
