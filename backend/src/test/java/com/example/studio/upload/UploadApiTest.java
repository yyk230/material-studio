package com.example.studio.upload;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.example.studio.IntegrationTestBase;

import jakarta.servlet.http.Cookie;

class UploadApiTest extends IntegrationTestBase {

    private ResultActions post(Cookie session, String templateId, String name, String type, byte[] content)
            throws Exception {
        return mvc.perform(multipart("/api/uploads")
                .file(new MockMultipartFile("file", name, type, content))
                .param("templateId", templateId)
                .cookie(session));
    }

    @Test
    void acceptsValidImageAndDetectsRealType() throws Exception {
        Cookie session = register("upload-ok@example.com");

        post(session, "spring-festival", "photo.bin", "application/octet-stream", jpeg())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.originalName").value("photo.bin"))
                .andExpect(jsonPath("$.url").exists());
    }

    @Test
    void rejectsFakeImage() throws Exception {
        Cookie session = register("upload-fake@example.com");

        post(session, "pet-sticker", "fake.png", "image/png", "not an image at all".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void rejectsTypeNotAcceptedByTemplate() throws Exception {
        Cookie session = register("upload-webp@example.com");

        post(session, "spring-festival", "photo.webp", "image/webp", webp())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"))
                .andExpect(jsonPath("$.message").value("仅支持 JPG/PNG 格式的图片"));
        post(session, "pet-sticker", "photo.webp", "image/webp", webp())
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsFileLargerThanTemplateLimit() throws Exception {
        Cookie session = register("upload-big@example.com");
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(png(), 0, big, 0, 8);

        post(session, "spring-festival", "big.png", "image/png", big)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value("图片不能超过 2 MB"));
    }

    @Test
    void rejectsEmptyFileAndUnknownTemplate() throws Exception {
        Cookie session = register("upload-empty@example.com");

        post(session, "pet-sticker", "empty.png", "image/png", new byte[0])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_EMPTY"));
        post(session, "no-such-template", "a.png", "image/png", png())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEMPLATE_NOT_FOUND"));
    }

    @Test
    void servesFilesOnlyToTheOwner() throws Exception {
        Cookie owner = register("upload-owner@example.com");
        Cookie other = register("upload-other@example.com");
        String uploadId = upload(owner, "pet-sticker", png());

        mvc.perform(get("/api/files/uploads/" + uploadId).cookie(owner))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/api/files/uploads/" + uploadId).cookie(other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
        mvc.perform(get("/api/files/uploads/" + uploadId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("file", "a.png", "image/png", png()))
                        .param("templateId", "pet-sticker"))
                .andExpect(status().isUnauthorized());
    }
}
