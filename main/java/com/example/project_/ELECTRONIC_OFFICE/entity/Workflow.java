package com.example.project_.ELECTRONIC_OFFICE.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "workflow")
public class Workflow {
    @Id
    @Column(name = "workflow_id", columnDefinition = "UUID", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID workflowId;

    @Column(name = "status")
    private String status;

    @Column(name = "create_by")
    private String createBy;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "description")
    private String description;

    @Column(name = "create_at")
    private OffsetDateTime createAt;

    @Column(name = "update_at")
    private OffsetDateTime updateAt;

    @Column(name = "nodes")
    private String nodes;

    @Column(name = "edges")
    private String edges;

    @Column(name = "version")
    private Integer version;

    @Column(name = "name")
    private String name;
    @PrePersist
    public void prePersist() {
        if (workflowId == null) {
            workflowId = UUID.randomUUID();
        }
        if (createAt == null) {
            createAt = OffsetDateTime.now();

        }
    }
}
