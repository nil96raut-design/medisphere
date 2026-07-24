package com.healthtrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import org.hibernate.annotations.Filter;

// A timestamped update/comment logged against a Task, forming the progress timeline.
@Entity
@Table(name = "progress_note")
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(length = 2000)
    private String note;

    // Snapshot of progress percent + status at the time this note was logged
    private Integer progressPercent;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;
}
