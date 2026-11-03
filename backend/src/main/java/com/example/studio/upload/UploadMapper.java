package com.example.studio.upload;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UploadMapper {

    @Insert("INSERT INTO upload_assets (id, user_id, template_id, original_name, mime_type, size_bytes, storage_path, created_at) "
            + "VALUES (#{id}, #{userId}, #{templateId}, #{originalName}, #{mimeType}, #{sizeBytes}, #{storagePath}, #{createdAt})")
    void insert(UploadAsset asset);

    @Select("SELECT * FROM upload_assets WHERE id = #{id}")
    UploadAsset findById(String id);
}
