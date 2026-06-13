package com.wanghao.eldercare.eldercaresystem.service.workflow;

import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfDefinition;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfDefinitionRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowDeployService implements ApplicationRunner {

    private final RepositoryService repositoryService;
    private final WfDefinitionRepository wfDefinitionRepository;

    public WorkflowDeployService(RepositoryService repositoryService,
                                 WfDefinitionRepository wfDefinitionRepository) {
        this.repositoryService = repositoryService;
        this.wfDefinitionRepository = wfDefinitionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncLatestDefinition("admission");
    }

    @Transactional
    public void syncLatestDefinition(String processKey) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
        if (definition == null) {
            return;
        }
        WfDefinition local = wfDefinitionRepository.findFirstByProcessKeyOrderByVersionDesc(processKey)
                .filter(existing -> definition.getId().equals(existing.getExternalProcessDefinitionId()))
                .orElseGet(WfDefinition::new);
        LocalDateTime now = LocalDateTime.now();
        if (local.getDefinitionId() == null) {
            local.setCreatedAt(now);
        }
        local.setProcessKey(definition.getKey());
        local.setProcessName(definition.getName());
        local.setVersion(definition.getVersion());
        local.setStatus("active");
        local.setEngineType("flowable");
        local.setExternalDeploymentId(definition.getDeploymentId());
        local.setExternalProcessDefinitionId(definition.getId());
        local.setDefinitionJson(readBpmnXml(definition));
        local.setDeploymentTime(resolveDeploymentTime(definition.getDeploymentId()));
        local.setUpdatedAt(now);
        wfDefinitionRepository.save(local);
    }

    private LocalDateTime resolveDeploymentTime(String deploymentId) {
        var deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        if (deployment == null || deployment.getDeploymentTime() == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(deployment.getDeploymentTime().toInstant(), ZoneId.systemDefault());
    }

    private String readBpmnXml(ProcessDefinition definition) {
        try (var stream = repositoryService.getResourceAsStream(
                definition.getDeploymentId(),
                definition.getResourceName())) {
            if (stream == null) {
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }
}
