package com.example.studio.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.studio.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.Cookie;

class TaskApiTest extends IntegrationTestBase {

    @Test
    void acceptsTaskWithoutWaitingForProcessing() throws Exception {
        simulation.update(null, 1000);
        Cookie session = register("accept@example.com");
        String uploadId = upload(session, "pet-sticker", png());

        MvcResult created = createTask(session, "pet-sticker", List.of(uploadId));

        assertThat(created.getResponse().getStatus()).isEqualTo(202);
        JsonNode body = json(created);
        String taskId = body.get("id").asText();
        assertThat(body.get("status").asText()).isEqualTo("PENDING");
        assertThat(body.get("steps")).isEmpty();
        assertThat(body.get("templateTitle").asText()).isEqualTo("萌宠贴纸");
        String dbStatus = jdbc.queryForObject("SELECT status FROM generation_tasks WHERE id = ?", String.class, taskId);
        assertThat(dbStatus).isNotEqualTo("SUCCEEDED");

        List<String> observed = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).until(() -> {
            JsonNode node = getTask(session, taskId);
            String status = node.get("status").asText();
            if (observed.isEmpty() || !observed.get(observed.size() - 1).equals(status)) {
                observed.add(status);
            }
            return status.equals("SUCCEEDED");
        });
        assertThat(observed).contains("PROCESSING", "SUCCEEDED");

        JsonNode done = getTask(session, taskId);
        assertThat(done.get("resultUrl").asText()).isEqualTo("/api/files/results/" + taskId);
        assertThat(done.get("steps")).hasSize(3);
        mvc.perform(get("/api/files/results/" + taskId).cookie(session))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Security-Policy", "sandbox"));
    }

    @Test
    void rejectsWrongImageCount() throws Exception {
        Cookie session = register("count@example.com");
        String uploadId = upload(session, "travel-postcard", png());

        MvcResult result = createTask(session, "travel-postcard", List.of(uploadId));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).get("code").asText()).isEqualTo("IMAGE_COUNT_MISMATCH");
    }

    @Test
    void rejectsDuplicateOrForeignUploads() throws Exception {
        Cookie alice = register("alice@example.com");
        Cookie bob = register("bob@example.com");
        String aliceUpload = upload(alice, "travel-postcard", png());
        String bobUpload = upload(bob, "travel-postcard", png());

        MvcResult duplicate = createTask(alice, "travel-postcard", List.of(aliceUpload, aliceUpload));
        assertThat(duplicate.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(duplicate).get("code").asText()).isEqualTo("INVALID_UPLOAD");

        MvcResult foreign = createTask(alice, "travel-postcard", List.of(aliceUpload, bobUpload));
        assertThat(foreign.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(foreign).get("code").asText()).isEqualTo("INVALID_UPLOAD");
    }

    @Test
    void rejectsUploadFromAnotherTemplate() throws Exception {
        Cookie session = register("other-template@example.com");
        String uploadId = upload(session, "pixel-avatar", png());

        MvcResult result = createTask(session, "pet-sticker", List.of(uploadId));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(result).get("code").asText()).isEqualTo("INVALID_UPLOAD");
    }

    @Test
    void rejectsUnknownTemplate() throws Exception {
        Cookie session = register("unknown@example.com");

        MvcResult result = createTask(session, "no-such-template", List.of("x"));

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(json(result).get("code").asText()).isEqualTo("TEMPLATE_NOT_FOUND");
    }

    @Test
    void isolatesTasksBetweenUsers() throws Exception {
        Cookie alice = register("iso-alice@example.com");
        Cookie bob = register("iso-bob@example.com");
        String uploadId = upload(alice, "pet-sticker", png());
        String taskId = json(createTask(alice, "pet-sticker", List.of(uploadId))).get("id").asText();
        awaitTerminal(alice, taskId);

        mvc.perform(get("/api/tasks/" + taskId).cookie(bob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
        mvc.perform(get("/api/files/results/" + taskId).cookie(bob))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/tasks").cookie(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/tasks").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(taskId));
        mvc.perform(get("/api/tasks/" + taskId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void paginatesAndFiltersByTemplate() throws Exception {
        Cookie session = register("page@example.com");
        String sticker = upload(session, "pet-sticker", png());
        String avatar = upload(session, "pixel-avatar", png());
        List<String> ids = new ArrayList<>();
        ids.add(json(createTask(session, "pet-sticker", List.of(sticker))).get("id").asText());
        ids.add(json(createTask(session, "pet-sticker", List.of(sticker))).get("id").asText());
        ids.add(json(createTask(session, "pixel-avatar", List.of(avatar))).get("id").asText());
        for (String id : ids) {
            awaitTerminal(session, id);
        }

        mvc.perform(get("/api/tasks").param("size", "2").cookie(session))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(true));
        mvc.perform(get("/api/tasks").param("size", "2").param("page", "1").cookie(session))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));
        mvc.perform(get("/api/tasks").param("templateId", "pet-sticker").cookie(session))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].steps.length()").value(3));
        mvc.perform(get("/api/tasks").param("size", "51").cookie(session))
                .andExpect(status().isBadRequest());
    }
}
