package com.example.menu.repository;

import com.example.menu.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByCategoryId(Long categoryId);
    List<Menu> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String desc);
}
