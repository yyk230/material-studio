package com.example.studio.template;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return templateService.categories();
    }

    @GetMapping
    public List<TemplateDto> list(@RequestParam(required = false) String category) {
        return templateService.list(category);
    }

    @GetMapping("/{id}")
    public TemplateDto get(@PathVariable String id) {
        return templateService.get(id);
    }
}
