package com.example.studio.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.example.studio.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.Cookie;

class DevSimulationApiTest extends IntegrationTestBase {

    @Test
    void configurationIsSnapshottedIntoNewTasks() throws Exception {
        mvc.perform(put("/api/dev/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("failStage", "GENERATION", "stepDelayMs", 10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failStage").value("GENERATION"))
                .andExpect(jsonPath("$.stepDelayMs").value(10));

        Cookie session = register("sim@example.com");
        String uploadId = upload(session, "pet-sticker", png());
        String taskId = json(createTask(session, "pet-sticker", List.of(uploadId))).get("id").asText();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT sim_fail_stage, sim_step_delay_ms FROM generation_tasks WHERE id = ?", taskId);
        assertThat(row.get("sim_fail_stage")).isEqualTo("GENERATION");
        assertThat(((Number) row.get("sim_step_delay_ms")).intValue()).isEqualTo(10);

        JsonNode done = awaitTerminal(session, taskId);
        assertThat(done.get("status").asText()).isEqualTo("FAILED");
        assertThat(done.get("currentStage").asText()).isEqualTo("GENERATION");
        assertThat(done.get("errorMessage").asText()).isEqualTo("生成服务暂时不可用（模拟失败）");
    }

    @Test
    void rejectsOutOfRangeDelay() throws Exception {
        mvc.perform(put("/api/dev/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("stepDelayMs", 60_001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/dev/simulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"failStage\":\"NOPE\",\"stepDelayMs\":10}"))
                .andExpect(status().isBadRequest());
    }
}
