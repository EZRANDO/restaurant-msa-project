package com.example.order.controller;

import com.example.order.domain.CartItem;
import com.example.order.repository.CartItemRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "장바구니 조회, 담기, 수량 변경, 삭제")
public class CartController {

    private final CartItemRepository cartItemRepository;

    @GetMapping
    public List<CartItem> getCart(@RequestHeader("X-User-Id") Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    @PostMapping
    public ResponseEntity<CartItem> addToCart(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Id") Long userId) {
        Long menuId = Long.valueOf(body.get("menuId").toString());
        String menuName = (String) body.get("menuName");
        Integer menuPrice = Integer.valueOf(body.get("menuPrice").toString());
        Integer quantity = Integer.valueOf(body.get("quantity").toString());

        CartItem item = cartItemRepository.findByUserIdAndMenuId(userId, menuId)
                .orElseGet(() -> {
                    CartItem c = new CartItem();
                    c.setUserId(userId);
                    c.setMenuId(menuId);
                    c.setMenuName(menuName);
                    c.setMenuPrice(menuPrice);
                    c.setQuantity(0);
                    return c;
                });
        item.setQuantity(item.getQuantity() + quantity);
        return ResponseEntity.ok(cartItemRepository.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartItem> updateQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            @RequestHeader("X-User-Id") Long userId) {
        CartItem item = cartItemRepository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다."));
        int qty = body.get("quantity");
        if (qty <= 0) {
            cartItemRepository.delete(item);
            return ResponseEntity.noContent().build();
        }
        item.setQuantity(qty);
        return ResponseEntity.ok(cartItemRepository.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        CartItem item = cartItemRepository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("장바구니 항목을 찾을 수 없습니다."));
        cartItemRepository.delete(item);
        return ResponseEntity.noContent().build();
    }
}
