package com.example.studio.template;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemplateMapper {

    @Select("""
            <script>
            SELECT * FROM templates
            <if test="category != null and category != ''">WHERE category = #{category}</if>
            ORDER BY sort_order
            </script>
            """)
    List<Template> findAll(@Param("category") String category);

    @Select("SELECT * FROM templates WHERE id = #{id}")
    Template findById(String id);

    @Select("SELECT category FROM templates GROUP BY category ORDER BY MIN(sort_order)")
    List<String> findCategories();
}
