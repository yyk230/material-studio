package com.example.studio.template;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.studio.common.ApiException;
import com.example.studio.common.ErrorCode;

@Service
public class TemplateService {

    private final TemplateMapper templateMapper;

    public TemplateService(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public List<String> categories() {
        return templateMapper.findCategories();
    }

    public List<TemplateDto> list(String category) {
        return templateMapper.findAll(category).stream().map(TemplateDto::from).toList();
    }

    public TemplateDto get(String id) {
        return TemplateDto.from(require(id));
    }

    public Template require(String id) {
        Template template = templateMapper.findById(id);
        if (template == null) {
            throw new ApiException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在");
        }
        return template;
    }
}
