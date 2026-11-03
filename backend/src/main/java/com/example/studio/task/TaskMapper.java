package com.example.studio.task;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMapper {

    String SELECT_WITH_TITLE = "SELECT t.*, tp.title AS template_title FROM generation_tasks t "
            + "JOIN templates tp ON tp.id = t.template_id ";

    @Insert("INSERT INTO generation_tasks (id, user_id, template_id, status, current_stage, error_message, result_url, "
            + "sim_fail_stage, sim_step_delay_ms, created_at, started_at, finished_at, updated_at) VALUES "
            + "(#{id}, #{userId}, #{templateId}, #{status}, #{currentStage}, #{errorMessage}, #{resultUrl}, "
            + "#{simFailStage}, #{simStepDelayMs}, #{createdAt}, #{startedAt}, #{finishedAt}, #{updatedAt})")
    void insert(GenerationTask task);

    @Insert("INSERT INTO task_inputs (task_id, input_index, upload_id) VALUES (#{taskId}, #{index}, #{uploadId})")
    void insertInput(@Param("taskId") String taskId, @Param("index") int index, @Param("uploadId") String uploadId);

    @Select("SELECT upload_id FROM task_inputs WHERE task_id = #{taskId} ORDER BY input_index")
    List<String> findInputUploadIds(String taskId);

    @Select(SELECT_WITH_TITLE + "WHERE t.id = #{id}")
    GenerationTask findById(String id);

    @Select(SELECT_WITH_TITLE + "WHERE t.id = #{id} AND t.user_id = #{userId}")
    GenerationTask findByIdAndUser(@Param("id") String id, @Param("userId") long userId);

    @Select("""
            <script>
            SELECT t.*, tp.title AS template_title FROM generation_tasks t
            JOIN templates tp ON tp.id = t.template_id
            WHERE t.user_id = #{userId}
            <if test="templateId != null and templateId != ''">AND t.template_id = #{templateId}</if>
            ORDER BY t.created_at DESC, t.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<GenerationTask> findPage(@Param("userId") long userId, @Param("templateId") String templateId,
                                  @Param("limit") int limit, @Param("offset") int offset);

    @Update("UPDATE generation_tasks SET status = 'PROCESSING', started_at = #{now}, updated_at = #{now} "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int markProcessing(@Param("id") String id, @Param("now") Instant now);

    @Update("UPDATE generation_tasks SET current_stage = #{stage}, updated_at = #{now} "
            + "WHERE id = #{id} AND status = 'PROCESSING'")
    int updateStage(@Param("id") String id, @Param("stage") TaskStage stage, @Param("now") Instant now);

    @Update("UPDATE generation_tasks SET status = 'FAILED', error_message = #{message}, finished_at = #{now}, "
            + "updated_at = #{now} WHERE id = #{id} AND status = 'PROCESSING'")
    int markFailed(@Param("id") String id, @Param("message") String message, @Param("now") Instant now);

    @Update("UPDATE generation_tasks SET status = 'FAILED', error_message = #{message}, finished_at = #{now}, "
            + "updated_at = #{now} WHERE id = #{id} AND status = 'PENDING'")
    int failIfPending(@Param("id") String id, @Param("message") String message, @Param("now") Instant now);

    @Update("UPDATE generation_tasks SET status = 'SUCCEEDED', result_url = #{resultUrl}, finished_at = #{now}, "
            + "updated_at = #{now} WHERE id = #{id} AND status = 'PROCESSING'")
    int markSucceeded(@Param("id") String id, @Param("resultUrl") String resultUrl, @Param("now") Instant now);

    @Update("UPDATE task_steps SET status = 'FAILED', message = #{message}, finished_at = #{now} "
            + "WHERE status = 'RUNNING' AND task_id IN (SELECT id FROM generation_tasks "
            + "WHERE created_at < #{before} AND status IN ('PENDING', 'PROCESSING'))")
    int failRunningStepsCreatedBefore(@Param("before") Instant before, @Param("message") String message,
                                      @Param("now") Instant now);

    @Update("UPDATE generation_tasks SET status = 'FAILED', error_message = #{message}, finished_at = #{now}, "
            + "updated_at = #{now} WHERE created_at < #{before} AND status IN ('PENDING', 'PROCESSING')")
    int failUnfinishedCreatedBefore(@Param("before") Instant before, @Param("message") String message,
                                    @Param("now") Instant now);
}
