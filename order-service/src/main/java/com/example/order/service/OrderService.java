package com.example.order.service;

import com.example.order.domain.Coupon;
import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.dto.OrderRequest;
import com.example.order.repository.CartItemRepository;
import com.example.order.repository.CouponRepository;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public Order create(Long userId, OrderRequest req) {
        Order order = new Order();
        order.setUserId(userId);

        int total = req.getItems().stream()
                .mapToInt(i -> i.getPrice() * i.getQuantity()).sum();
        order.setTotalPrice(total);

        int discount = 0;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            Coupon coupon = couponRepository.findByCode(req.getCouponCode())
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 쿠폰입니다."));
            if (coupon.getExpiryDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("만료된 쿠폰입니다.");
            if (total < coupon.getMinOrderAmount()) throw new IllegalArgumentException("최소 주문 금액 미달입니다.");

            discount = coupon.getDiscountType() == Coupon.DiscountType.FIXED
                    ? coupon.getDiscountValue()
                    : total * coupon.getDiscountValue() / 100;
            order.setCouponId(coupon.getId());
        }

        order.setDiscountAmount(discount);
        order.setFinalPrice(total - discount);
        Order saved = orderRepository.save(order);

        for (OrderRequest.OrderItemDto dto : req.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(saved);
            item.setMenuId(dto.getMenuId());
            item.setMenuName(dto.getMenuName());
            item.setQuantity(dto.getQuantity());
            item.setPrice(dto.getPrice());
            orderItemRepository.save(item);
        }

        cartItemRepository.deleteByUserId(userId);
        return saved;
    }

    public List<Order> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        order.setStatus(Order.OrderStatus.valueOf(status));
        return orderRepository.save(order);
    }

    public Map<String, Object> getStats(String type, String value) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime[] range = switch (type) {
            case "daily" -> {
                LocalDate date = LocalDate.parse(value);
                yield new LocalDateTime[]{date.atStartOfDay(), date.plusDays(1).atStartOfDay()};
            }
            case "monthly" -> {
                YearMonth month = YearMonth.parse(value);
                yield new LocalDateTime[]{month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()};
            }
            case "yearly" -> {
                Year year = Year.parse(value);
                yield new LocalDateTime[]{year.atDay(1).atStartOfDay(), year.plusYears(1).atDay(1).atStartOfDay()};
            }
            default -> throw new IllegalArgumentException("type은 daily/monthly/yearly 중 하나여야 합니다.");
        };
        Integer revenue = orderRepository.sumBetween(range[0], range[1], Order.OrderStatus.PENDING);
        result.put("type", type);
        result.put("value", value);
        result.put("revenue", revenue != null ? revenue : 0);

        List<Object[]> popular = orderItemRepository.findPopularMenus();
        result.put("popularMenus", popular.stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("menuId", row[0]);
            m.put("menuName", row[1]);
            m.put("totalQuantity", row[2]);
            return m;
        }).toList());
        return result;
    }
}
