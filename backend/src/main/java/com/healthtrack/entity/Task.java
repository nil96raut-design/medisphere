package com.healthtrack.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "task")
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(length = 2000)
    private String description;

    // Who must do the task (usually the patient, but can be staff too)
    @ManyToOne(optional = false)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    // Who created/assigned the task (usually the doctor or staff)
    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    // 0-100, drives the patient's progress view
    @Builder.Default
    private Integer progressPercent = 0;

    private LocalDate dueDate;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;
}
