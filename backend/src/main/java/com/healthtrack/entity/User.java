package com.healthtrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "app_user")
@Filter(name = "tenantFilter", condition = "hospital_id = :hospitalId")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Free-text allergy/contraindication notes, patient-only in practice.
    // Not exposed via /api/users/by-role — only clinical endpoints that need it
    // (e.g. prescribing) should read this directly off the entity/DTO.
    @Column(length = 1000)
    private String allergies;

    @ManyToOne
    @JoinColumn(name = "primary_doctor_id")
    @JsonIgnore
    private User primaryDoctor;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    @OneToMany(mappedBy = "primaryDoctor")
    @JsonIgnore
    private Set<User> patients = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    @JsonIgnore
    private Hospital hospital;
}
