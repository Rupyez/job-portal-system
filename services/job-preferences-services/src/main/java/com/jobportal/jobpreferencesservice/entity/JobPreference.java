
package com.jobportal.jobpreferencesservice.entity;

import com.jobportal.jobpreferencesservice.enums.EmploymentType;
import com.jobportal.jobpreferencesservice.enums.WorkMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "job_preferences",
        indexes = {
                @Index(
                        name = "idx_job_preferences_user_id",
                        columnList = "user_id",
                        unique = true
                )
        }
)

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPreference {

    private static final int CURRENCY_CODE_LENGTH = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;




    // ---------------------------------------------------------------------
    // Collection fields
    // --------------------------------------------------------------------
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_preference_titles", joinColumns = @JoinColumn(name = "job_preference_id"))
    @Column(name = "job_title", nullable = false, length = 200)
    @Builder.Default
    private List<String> preferredJobTitles = new ArrayList<>();


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_preference_locations", joinColumns = @JoinColumn(name = "job_preference_id"))
    @Column(name = "location", nullable = false, length = 150)
    @Builder.Default
    private List<String> preferredLocations = new ArrayList<>();


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_preference_skills", joinColumns = @JoinColumn(name = "job_preference_id"))
    @Column(name = "skill", nullable = false, length = 100)
    @Builder.Default
    private List<String> preferredSkills = new ArrayList<>();


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_preference_industries", joinColumns = @JoinColumn(name = "job_preference_id"))
    @Column(name = "industry", nullable = false, length = 150)
    @Builder.Default
    private List<String> preferredIndustries = new ArrayList<>();

    // ---------------------------------------------------------------------
    // Scalar preference fields
    // ---------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", length = 30)
    private WorkMode workMode;

    @Column(name = "minimum_salary", precision = 12, scale = 2)
    private BigDecimal minimumSalary;

    @Column(name = "maximum_salary", precision = 12, scale = 2)
    private BigDecimal maximumSalary;

    @Column(name = "currency", length = CURRENCY_CODE_LENGTH)
    private String currency;


    /**
     * Replaces all mutable preference fields in one validated operation.
     *
     * <p>Callers pass the full desired state; there are no partial setters on
     * business fields, which keeps the aggregate consistent regardless of
     * which update path a client takes.
     */

    public void update(
            List<String> newJobTitles,
            List<String> newLocations,
            List<String> newSkills,
            List<String> newIndustries,
            EmploymentType newEmploymentType,
            WorkMode newWorkMode,
            BigDecimal newMinimumSalary,
            BigDecimal newMaximumSalary,
            String newCurrency
    ){
        validateSalaryRange(newMinimumSalary, newMaximumSalary, newCurrency);
        preferredJobTitles = sanitizeList(newJobTitles, 200, "preferredJobTitles");
        preferredLocations = sanitizeList(newLocations, 150, "preferredLocations");
        preferredSkills = sanitizeList(newSkills, 100, "preferredSkills");
        preferredIndustries = sanitizeList(newIndustries, 150, "preferredIndustries");


        employmentType = newEmploymentType;
        workMode = newWorkMode;
        minimumSalary = newMinimumSalary;
        maximumSalary = newMaximumSalary;
        currency = newCurrency;
    }
    // ---------------------------------------------------------------------
    // Domain operations
    // ---------------------------------------------------------------------

    /**
     * Replaces all mutable preference fields in one validated operation.
     *
     * <p>Callers pass the full desired state; there are no partial setters on
     * business fields, which keeps the aggregate consistent regardless of
     * which update path a client takes.
     */

    // ---------------------------------------------------------------------
    // JPA lifecycle
    // ---------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }


    // ---------------------------------------------------------------------
    // Validation helpers
    // --------------------------------------------------------------------
    private static void validateSalaryRange(BigDecimal min, BigDecimal max, String currency){
        if (min != null && min.signum() < 0) {
            throw new IllegalArgumentException("minimumSalary must be >= 0");
        }
        if (max != null && max.signum() < 0) {
            throw new IllegalArgumentException("maximumSalary must be >= 0");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("minimumSalary must be <= maximumSalary");
        }
        boolean hasSalary = min != null || max != null;
        if (hasSalary && (currency == null || currency.length() != CURRENCY_CODE_LENGTH)) {
            throw new IllegalArgumentException(
                    "currency must be a " + CURRENCY_CODE_LENGTH + "-letter ISO 4217 code when a salary is set");
        }
        if (currency != null && currency.length() != CURRENCY_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "currency must be a " + CURRENCY_CODE_LENGTH + "-letter ISO 4217 code");
        }
    }


    private static List<String> sanitizeList(List<String> values, int maxLength, String field){
        if(values == null || values.isEmpty()){
            return List.of();
        }

        return values.stream()
                .map(value -> {
                    if (value == null || value.isBlank()){
                        throw new IllegalArgumentException(field + "must not contain blank entries");
                    }
                    var trimmed = value.trim();
                    if (trimmed.length() > maxLength){
                        throw new IllegalArgumentException(field + " entry exceeds" + maxLength + "characters: " + trimmed);

                    }
                    return trimmed;
                }).toList();
    }
}
