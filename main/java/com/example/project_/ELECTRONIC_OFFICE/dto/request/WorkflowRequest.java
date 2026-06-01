package com.example.project_.ELECTRONIC_OFFICE.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class WorkflowRequest {
    @JsonProperty("companyId")
    private String companyId;
    private String name;
    private String description;
    private String nodes;
    private String edges;
    private String status;
    private Integer version;
    private String createdBy;
}
