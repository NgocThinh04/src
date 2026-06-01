package com.example.project_.ELECTRONIC_OFFICE.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class WorkflowResponse {
    private UUID workflowId;
    private String name;
    private String description;
    private Object nodes;
    private Object edges;
    private String status;
    private Integer version;
    private String createdBy;
    private String updateBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
