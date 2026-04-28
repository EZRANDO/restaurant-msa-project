package com.example.menu.service;

import com.example.menu.domain.Category;
import com.example.menu.domain.Menu;
import com.example.menu.dto.MenuRequest;
import com.example.menu.dto.MenuResponse;
import com.example.menu.repository.CategoryRepository;
import com.example.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;

    public List<MenuResponse> getAll(Long categoryId, String keyword) {
        List<Menu> menus;
        if (keyword != null && !keyword.isBlank()) {
            menus = menuRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
        } else if (categoryId != null) {
            menus = menuRepository.findByCategoryId(categoryId);
        } else {
            menus = menuRepository.findAll();
        }
        return menus.stream().map(MenuResponse::from).toList();
    }

    public MenuResponse getOne(Long id) {
        return MenuResponse.from(menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다.")));
    }

    public MenuResponse create(MenuRequest req) {
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
        Menu menu = new Menu();
        applyRequest(menu, req, category);
        return MenuResponse.from(menuRepository.save(menu));
    }

    public MenuResponse update(Long id, MenuRequest req) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다."));
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
        applyRequest(menu, req, category);
        return MenuResponse.from(menuRepository.save(menu));
    }

    public void delete(Long id) {
        if (!menuRepository.existsById(id)) throw new IllegalArgumentException("메뉴를 찾을 수 없습니다.");
        menuRepository.deleteById(id);
    }

    private void applyRequest(Menu menu, MenuRequest req, Category category) {
        menu.setName(req.getName());
        menu.setDescription(req.getDescription());
        menu.setPrice(req.getPrice());
        menu.setImageUrl(req.getImageUrl());
        menu.setCategory(category);
        if (req.getAvailable() != null) menu.setAvailable(req.getAvailable());
    }
}
