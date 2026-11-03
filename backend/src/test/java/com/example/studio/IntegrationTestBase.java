package com.example.studio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.studio.simulation.SimulationSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    /** 所有测试类共用同一存储目录，使 Spring 测试上下文可以被缓存复用。 */
    protected static final Path STORAGE;

    static {
        try {
            STORAGE = Files.createTempDirectory("studio-test-storage");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root", STORAGE::toString);
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected SimulationSettings simulation;
    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM task_steps");
        jdbc.update("DELETE FROM task_inputs");
        jdbc.update("DELETE FROM generation_tasks");
        jdbc.update("DELETE FROM upload_assets");
        jdbc.update("DELETE FROM user_sessions");
        jdbc.update("DELETE FROM users WHERE email NOT IN ('demo1@example.com', 'demo2@example.com')");
        simulation.reset();
    }

    protected Cookie register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", "Passw0rd!", "displayName", "tester"))))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getCookie("SESSION");
    }

    protected String upload(Cookie session, String templateId, byte[] content) throws Exception {
        MvcResult result = mvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", content))
                        .param("templateId", templateId)
                        .cookie(session))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asText();
    }

    protected MvcResult createTask(Cookie session, String templateId, List<String> uploadIds) throws Exception {
        return mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("templateId", templateId, "uploadIds", uploadIds)))
                        .cookie(session))
                .andReturn();
    }

    protected JsonNode getTask(Cookie session, String taskId) throws Exception {
        return json(mvc.perform(get("/api/tasks/" + taskId).cookie(session))
                .andExpect(status().isOk())
                .andReturn());
    }

    protected JsonNode awaitTerminal(Cookie session, String taskId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(50))
                .until(() -> getTask(session, taskId),
                        node -> node.get("status").asText().equals("SUCCEEDED")
                                || node.get("status").asText().equals("FAILED"));
    }

    protected JsonNode json(MvcResult result) throws IOException {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected String toJson(Object value) throws IOException {
        return objectMapper.writeValueAsString(value);
    }

    protected static byte[] png() {
        return withPadding(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
    }

    protected static byte[] jpeg() {
        return withPadding(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    }

    protected static byte[] webp() {
        return withPadding(new byte[] {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'});
    }

    private static byte[] withPadding(byte[] header) {
        byte[] data = new byte[header.length + 64];
        System.arraycopy(header, 0, data, 0, header.length);
        return data;
    }
}
