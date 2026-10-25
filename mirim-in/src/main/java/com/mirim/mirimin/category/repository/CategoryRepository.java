package com.mirim.mirimin.category.repository;

import com.mirim.mirimin.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOfficialTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc();

    Optional<Category> findByName(String name);
}
