package com.project.rentmebarwaha.enquiry.entity;

import com.project.rentmebarwaha.common.model.BaseAuditEntity;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "enquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enquiry extends BaseAuditEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(name = "preferred_move_in_date", nullable = false)
    private LocalDate preferredMoveInDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", nullable = false, length = 50)
    private TenantType tenantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "lease_duration", nullable = false, length = 50)
    private LeaseDuration leaseDuration;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "enquiry_visit_timings", joinColumns = @JoinColumn(name = "enquiry_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "visit_timing")
    @Builder.Default
    private Set<VisitTiming> visitTimings = new HashSet<>();

    @Column(name = "personal_note", columnDefinition = "TEXT")
    private String personalNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EnquiryStatus status = EnquiryStatus.NEW;

    @PrePersist
    @Override
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        super.onCreate();
    }
}

