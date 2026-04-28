package com.example.order.repository;

import com.example.order.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndMenuId(Long userId, Long menuId);
    void deleteByUserId(Long userId);
}
