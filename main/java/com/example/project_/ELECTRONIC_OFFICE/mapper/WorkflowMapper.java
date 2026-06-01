package com.example.project_.ELECTRONIC_OFFICE.mapper;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.WorkflowRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.WorkflowResponse;
import com.example.project_.ELECTRONIC_OFFICE.entity.Workflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowMapper {

    private final ObjectMapper objectMapper;


    public WorkflowResponse toReponse(Workflow workflow) {
        if (workflow == null) {
            return null;
        }

        WorkflowResponse dto = new WorkflowResponse();
        dto.setWorkflowId(workflow.getWorkflowId());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setStatus(workflow.getStatus());
        dto.setVersion(workflow.getVersion());
        dto.setCreatedBy(workflow.getCreateBy());
//        dto.setUpdatedBy(workflow.getUpdatedBy());
        dto.setCreatedAt(workflow.getCreateAt());
        dto.setUpdatedAt(workflow.getUpdateAt());

        // Parse JSON string sang Object
        parseNodesAndEdges(workflow, dto);

        return dto;
    }

    public Workflow toEntity(WorkflowRequest dto) {
        if (dto == null) {
            return null;
        }

        Workflow workflow = new Workflow();
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setStatus(dto.getStatus());
        workflow.setVersion(dto.getVersion());
        workflow.setCreateBy(dto.getCreatedBy());
        workflow.setNodes(dto.getNodes());
        workflow.setEdges(dto.getEdges());
        if (dto.getCompanyId() != null && !dto.getCompanyId().isEmpty()) {
            try {
                workflow.setCompanyId(UUID.fromString(dto.getCompanyId()));
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID format: {}", dto.getCompanyId(), e);
                throw new RuntimeException("Invalid companyId format: " + dto.getCompanyId());
            }
        }

        // Chuyển đổi Object sang JSON string
//        serializeNodesAndEdges(dto, workflow);

        return workflow;
    }

    /**
     * Cập nhật Entity từ DTO (cho update)
     */
    public void updateEntity(WorkflowRequest dto, Workflow workflow) {
        if (dto.getName() != null) {
            workflow.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            workflow.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            workflow.setStatus(dto.getStatus());
        }
//        if (dto.getUpdatedBy() != null) {
//            workflow.setUpdatedBy(dto.getUpdatedBy());
//        }

        // Cập nhật nodes và edges
        if (dto.getNodes() != null) {
            try {
                workflow.setNodes(objectMapper.writeValueAsString(dto.getNodes()));
            } catch (Exception e) {
                log.error("Error serializing nodes to JSON", e);
            }
        }

        if (dto.getEdges() != null) {
            try {
                workflow.setEdges(objectMapper.writeValueAsString(dto.getEdges()));
            } catch (Exception e) {
                log.error("Error serializing edges to JSON", e);
            }
        }

        // Tăng version khi cập nhật
        workflow.setVersion(workflow.getVersion() + 1);
    }

    /**
     * Parse nodes và edges từ JSON string sang Object
     */
    private void parseNodesAndEdges(Workflow workflow, WorkflowResponse dto) {
        try {
            if (workflow.getNodes() != null && !workflow.getNodes().isEmpty()) {
                dto.setNodes(objectMapper.readValue(workflow.getNodes(), Object.class).toString());
            } else {
                dto.setNodes(List.of().toString());
            }

            if (workflow.getEdges() != null && !workflow.getEdges().isEmpty()) {
                dto.setEdges(objectMapper.readValue(workflow.getEdges(), Object.class).toString());
            } else {
                dto.setEdges(List.of().toString());
            }
        } catch (Exception e) {
            log.error("Error parsing JSON from database for workflow id: {}", workflow.getWorkflowId(), e);
            dto.setNodes(List.of().toString());
            dto.setEdges(List.of().toString());
        }
    }

    /**
     * Serialize nodes và edges từ Object sang JSON string
     */
    private void serializeNodesAndEdges(WorkflowRequest dto, Workflow workflow) {
        try {
            if (dto.getNodes() != null) {
                workflow.setNodes(objectMapper.writeValueAsString(dto.getNodes()));
            } else {
                workflow.setNodes("[]");
            }

            if (dto.getEdges() != null) {
                workflow.setEdges(objectMapper.writeValueAsString(dto.getEdges()));
            } else {
                workflow.setEdges("[]");
            }
        } catch (Exception e) {
            log.error("Error serializing nodes/edges to JSON for workflow: {}", dto.getName(), e);
            workflow.setNodes("[]");
            workflow.setEdges("[]");
        }
    }
}
