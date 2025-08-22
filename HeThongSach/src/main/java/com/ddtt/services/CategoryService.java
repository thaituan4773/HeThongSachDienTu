package com.ddtt.services;

import com.ddtt.dtos.CategoryDTO;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class CategoryService {
    public List<CategoryDTO> categories = new ArrayList<>();
}
