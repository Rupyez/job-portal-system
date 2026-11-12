package com.jobportal.companyservice.repository;

import com.jobportal.companyservice.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
