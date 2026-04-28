package com.example.order.repository;

import com.example.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = "SELECT menu_id, menu_name, SUM(quantity) AS total FROM order_items GROUP BY menu_id, menu_name ORDER BY total DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findPopularMenus();
}
