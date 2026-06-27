package com.aftersales.platform.agent.service;

import com.aftersales.platform.agent.domain.ModelCallLog;
import com.aftersales.platform.agent.repository.ModelCallTraceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelCallTraceService {
    private final ModelCallTraceRepository repository;

    public ModelCallTraceService(ModelCallTraceRepository repository) {
        this.repository = repository;
    }

    public void record(Long runId, String scene, String modelName, long elapsedMs,
                       String status, String errorMessage) {
        if (runId != null) {
            repository.save(runId, scene, modelName, elapsedMs, status, errorMessage);
        }
    }

    public List<ModelCallLog> findByRunId(Long runId) {
        return repository.findByRunId(runId);
    }
}
