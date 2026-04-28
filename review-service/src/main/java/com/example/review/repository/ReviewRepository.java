package com.example.review.repository;

import com.example.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMenuId(Long menuId);
    List<Review> findByUserId(Long userId);
}
