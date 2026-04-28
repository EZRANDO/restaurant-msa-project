package com.example.order.controller;

import com.example.order.domain.Coupon;
import com.example.order.repository.CouponRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "쿠폰 등록, 수정, 삭제, 검증")
public class CouponController {

    private final CouponRepository couponRepository;

    @GetMapping
    public ResponseEntity<List<Coupon>> getAll(
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Coupon> create(
            @RequestBody Coupon coupon,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Coupon> update(
            @PathVariable Long id,
            @RequestBody Coupon updated,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));
        coupon.setCode(updated.getCode());
        coupon.setDiscountType(updated.getDiscountType());
        coupon.setDiscountValue(updated.getDiscountValue());
        coupon.setMinOrderAmount(updated.getMinOrderAmount());
        coupon.setExpiryDate(updated.getExpiryDate());
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        couponRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<Coupon> validate(@PathVariable String code) {
        return couponRepository.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
