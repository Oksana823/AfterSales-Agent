package com.aftersales.platform.agent.repository;

import com.aftersales.platform.agent.domain.*;
import com.aftersales.platform.common.domain.Enums.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class TraceRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<AgentRun> runMapper = (rs, n) -> new AgentRun(rs.getLong("id"), rs.getString("user_input"), TaskType.valueOf(rs.getString("task_type")), RunStatus.valueOf(rs.getString("status")), rs.getString("final_answer"), rs.getObject("replay_from_run_id", Long.class), rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    private final RowMapper<AgentStep> stepMapper = (rs, n) -> new AgentStep(rs.getLong("id"), rs.getLong("run_id"), rs.getString("agent_name"), rs.getString("step_name"), rs.getString("result"), rs.getTimestamp("created_at").toLocalDateTime());
    private final RowMapper<ToolCallLog> toolMapper = (rs, n) -> new ToolCallLog(rs.getLong("id"), rs.getLong("run_id"), rs.getString("tool_name"), rs.getString("arguments_json"), rs.getString("result_json"), rs.getLong("elapsed_ms"), rs.getString("status"), rs.getString("error_message"), rs.getTimestamp("created_at").toLocalDateTime());
    public TraceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Long createRun(String input, TaskType type, Long replayFromRunId) { GeneratedKeyHolder kh = new GeneratedKeyHolder(); jdbc.update(c -> { PreparedStatement ps = c.prepareStatement("insert into agent_run(user_input,task_type,status,replay_from_run_id,created_at,updated_at) values(?,?,?,?,now(),now())", Statement.RETURN_GENERATED_KEYS); ps.setString(1, input); ps.setString(2, type.name()); ps.setString(3, RunStatus.RUNNING.name()); if (replayFromRunId == null) ps.setObject(4, null); else ps.setLong(4, replayFromRunId); return ps; }, kh); return kh.getKey().longValue(); }
    public void updateTaskType(Long runId, TaskType type) { jdbc.update("update agent_run set task_type=?, updated_at=now() where id=?", type.name(), runId); }
    public void updateRun(Long runId, RunStatus status, String answer) { jdbc.update("update agent_run set status=?, final_answer=?, updated_at=now() where id=?", status.name(), answer, runId); }
    public void addStep(Long runId, String agent, String step, String result) { jdbc.update("insert into agent_step(run_id,agent_name,step_name,result,created_at) values(?,?,?,?,now())", runId, agent, step, result); }
    public void addToolCall(Long runId, String toolName, String args, String result, long elapsedMs, String status, String error) { jdbc.update("insert into tool_call_log(run_id,tool_name,arguments_json,result_json,elapsed_ms,status,error_message,created_at) values(?,?,?,?,?,?,?,now())", runId, toolName, args, result, elapsedMs, status, error); }
    public Optional<AgentRun> findRun(Long runId) { return jdbc.query("select * from agent_run where id=?", runMapper, runId).stream().findFirst(); }
    public List<AgentStep> findSteps(Long runId) { return jdbc.query("select * from agent_step where run_id=? order by id", stepMapper, runId); }
    public List<ToolCallLog> findToolCalls(Long runId) { return jdbc.query("select * from tool_call_log where run_id=? order by id", toolMapper, runId); }
}
