package com.example.review.controller;

import com.example.review.domain.Review;
import com.example.review.repository.ReviewRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "리뷰 조회, 작성, 수정, 삭제")
public class ReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping
    public List<Review> getAll(@RequestParam(required = false) Long menuId) {
        return menuId != null ? reviewRepository.findByMenuId(menuId) : reviewRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Review> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Email") String email) {
        Review review = new Review();
        review.setUserId(userId);
        review.setUserEmail(email);
        review.setMenuId(Long.valueOf(body.get("menuId").toString()));
        review.setRating(Integer.valueOf(body.get("rating").toString()));
        review.setComment((String) body.get("comment"));
        return ResponseEntity.ok(reviewRepository.save(review));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        if (!review.getUserId().equals(userId) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        if (body.containsKey("rating")) review.setRating(Integer.valueOf(body.get("rating").toString()));
        if (body.containsKey("comment")) review.setComment((String) body.get("comment"));
        return ResponseEntity.ok(reviewRepository.save(review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        if (!review.getUserId().equals(userId) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        reviewRepository.delete(review);
        return ResponseEntity.noContent().build();
    }
}
