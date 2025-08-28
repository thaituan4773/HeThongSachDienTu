package com.ddtt.controllers;

import com.ddtt.dtos.TagDTO;
import com.ddtt.services.TagService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiTag {
    
    private final TagService tagService;
    
    @Get("/tags")
    public HttpResponse<List<TagDTO>> getTags() {
        return HttpResponse.ok(tagService.getAllTags());
    }
}
