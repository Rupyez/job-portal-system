package com.jobportal.jobservice.entity;

import com.jobportal.jobservice.enums.EmploymentType;
import com.jobportal.jobservice.enums.JobStatus;


import com.jobportal.jobservice.enums.WorkPlaceType;
import jakarta.persistence.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


/**
 * Aggregate root representing a job posting.
 *
 * <p><b>Scalability notes</b>
 * <ul>
 *   <li>New fields are added as columns + Bean Validation; no constructor churn
 *       because construction goes through {@link create}.</li>
 *   <li>New lifecycle states are added to {@link JobStatus} and gated through
 *      the entity does not need to change shape.</li>
 *   <li>New domain operations follow the {@code updateXxx}/{@code transitionTo}
 *       pattern and reuse the shared validators, so validation stays in one place.</li>
 *   <li>{@code @Data} is constrained: framework fields are write-protected and
 *       equality/toString are pinned to safe definitions so the entity behaves
 *       correctly inside sets, maps, logs, and Hibernate sessions.</li>
 * </ul>
 */


@Entity
@Table(name = "jobs",
            indexes = {
                        @Index(name = "idx_jobs_employeer_id", columnList ="employerId"),
                        @Index(name = "idx_jobs_status", columnList = "status"),
                        @Index(name = "idx_jobs_location", columnList = "location"),
                        @Index(name = "idx_jobs_employment_type", columnList = "employmentType"),
                        @Index(name = "idx_workplace_type", columnList = "workplaceType"),
                        @Index(name = "idx_jobs_created_at", columnList = "createdAt")
            }
)
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "description")


public class Job {

    /** ISO 4217 currency codes are always three letters. */
    private static final int CURRENCY_CODE_LENGTH = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.NONE)
    private UUID id;


    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;


    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;


    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;


    @NotNull
    @Column(nullable = false)
    private UUID employerId;


    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String location;


    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentType employmentType;



    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkPlaceType workPlaceType;


    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;


    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;


    @Size(min = CURRENCY_CODE_LENGTH, max = CURRENCY_CODE_LENGTH)
    @Column(length = CURRENCY_CODE_LENGTH)
    private String salaryCurrency;


    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status;


    // ---------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------

    /**
     * Creates a new job posting in {@link JobStatus#DRAFT}. All required
     * fields are supplied up front, so a {@code Job} can never exist in a
     * half-initialized state.
     *
     * <p>As the model grows, keep this factory as the single entry point and
     * add new parameters (or switch to a builder) here — call sites stay
     * unchanged because they already funnel through this method.
     */

    public static Job create(
            String title,
            String description,
            UUID employerId,
            String location,
            EmploymentType employmentType,
            WorkPlaceType workPlaceType
    ){
        Job job = new Job();

        job.title = requireText(title, "title");
        job.description = requireText(description, "description");
        job.employerId = Objects.requireNonNull(employerId, "employerId");
        job.location = requireText(location, "location");
        job.employmentType = Objects.requireNonNull(employmentType, "employmentType");
        job.workPlaceType = Objects.requireNonNull(workPlaceType, "workplaceType");
        job.status = JobStatus.DRAFT;
        return job;
    }

    private static @NotBlank @Size(max = 200) String requireText(String value, String field) {
        if(value == null || value.isBlank()){
            throw new IllegalArgumentException(field + "must not be blank");
        }
        return value;
    }


    // ---------------------------------------------------------------------
    // Domain operations
    // ---------------------------------------------------------------------
    public void updateDetails(String newTitle, String newDescription, String newLocation){
        title = requireText(newTitle, "title");
        description = requireText(newDescription, "description");
        location = requireText(newLocation, "location");
    }

    public void updateClassification(EmploymentType newEmploymentType, WorkPlaceType newWorkPlaceType) {
        employmentType = Objects.requireNonNull(newEmploymentType, "employmentType");
        workPlaceType = Objects.requireNonNull(newWorkPlaceType, "workPlaceType");
    }


    public void updateCompensation(BigDecimal newMin, BigDecimal newMax, String newCurrency){
        validateCompensation(newMin, newMax, newCurrency);
        salaryMin = newMin;
        salaryMax = newMax;
        salaryCurrency = newCurrency;
    }


    public void publish() {
        transitionTo(JobStatus.PUBLISHED);
    }

    public void close() {
        transitionTo(JobStatus.CLOSED);
    }




    /**
     * Single choke point for status changes. New states only need a new
     * {@code case} here plus an enum constant — call sites stay unchanged.
     */


    private void transitionTo(JobStatus target) {
        if (!isTransitionAllowed(status, target)) {
            throw new IllegalStateException(
                    "Cannot transition job from " + status + " to " + target);
        }
        status = target;
    }

    private static boolean isTransitionAllowed(JobStatus from, JobStatus to) {
        return switch (from) {
            case DRAFT     -> to == JobStatus.PUBLISHED;
            case PUBLISHED -> to == JobStatus.CLOSED;
            case CLOSED    -> false;
            case ARCHIVED  -> false;   // terminal
            case PAUSED    -> to == JobStatus.PUBLISHED || to == JobStatus.CLOSED;
        };
    }




    // ---------------------------------------------------------------------
    // JPA lifecycle
    // ---------------------------------------------------------------------
    @PrePersist
    protected void onCreate(){
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
        status = Objects.requireNonNullElse(status, JobStatus.DRAFT);
    }


    @PreUpdate
    protected void onUpdate(){
        updatedAt = Instant.now();
    }


    private void validateCompensation(BigDecimal min, BigDecimal max, String currency) {
        if(min != null && min.signum() < 0){
            throw new IllegalArgumentException("SalaryMin must be >= 0");
        }

        if(max != null && max.signum() < 0){
            throw new IllegalArgumentException("SalaryMax must be >= 0");
        }

        if(min != null && max != null && min.compareTo(max) > 0){
            throw new IllegalArgumentException("SalaryMin must be <= salaryMax");
        }

        if (currency != null && currency.length() != CURRENCY_CODE_LENGTH){
            throw new IllegalArgumentException("salaryCurrency must be a " + CURRENCY_CODE_LENGTH + "-letter ISO 4217 code");
        }
    }

}