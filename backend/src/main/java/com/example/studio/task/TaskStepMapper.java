package com.example.studio.task;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskStepMapper {

    @Insert("INSERT INTO task_steps (task_id, stage, status, started_at) VALUES (#{taskId}, #{stage}, 'RUNNING', #{now})")
    void insertRunning(@Param("taskId") String taskId, @Param("stage") TaskStage stage, @Param("now") Instant now);

    @Update("UPDATE task_steps SET status = 'SUCCEEDED', finished_at = #{now} "
            + "WHERE task_id = #{taskId} AND stage = #{stage} AND status = 'RUNNING'")
    int markSucceeded(@Param("taskId") String taskId, @Param("stage") TaskStage stage, @Param("now") Instant now);

    @Update("UPDATE task_steps SET status = 'FAILED', message = #{message}, finished_at = #{now} "
            + "WHERE task_id = #{taskId} AND stage = #{stage} AND status = 'RUNNING'")
    int markFailed(@Param("taskId") String taskId, @Param("stage") TaskStage stage,
                   @Param("message") String message, @Param("now") Instant now);

    @Select("SELECT * FROM task_steps WHERE task_id = #{taskId} ORDER BY id")
    List<TaskStep> findByTaskId(String taskId);

    @Select("""
            <script>
            SELECT * FROM task_steps WHERE task_id IN
            <foreach collection="taskIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            </script>
            """)
    List<TaskStep> findByTaskIds(@Param("taskIds") Collection<String> taskIds);
}
