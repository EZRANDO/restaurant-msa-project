package com.example.menu.controller;

import com.example.menu.dto.MenuRequest;
import com.example.menu.dto.MenuResponse;
import com.example.menu.service.MenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
@Tag(name = "Menus", description = "메뉴 등록, 수정, 삭제, 조회")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public List<MenuResponse> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return menuService.getAll(categoryId, keyword);
    }

    @GetMapping("/{id}")
    public MenuResponse getOne(@PathVariable Long id) {
        return menuService.getOne(id);
    }

    @PostMapping
    public ResponseEntity<MenuResponse> create(
            @Valid @RequestBody MenuRequest req,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(menuService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MenuRequest req,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(menuService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", defaultValue = "") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        menuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
