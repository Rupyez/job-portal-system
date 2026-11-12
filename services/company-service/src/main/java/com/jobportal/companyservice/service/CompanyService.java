package com.jobportal.companyservice.service;

import com.jobportal.companyservice.dto.CompanyRequest;
import com.jobportal.companyservice.dto.CompanyResponse;

import java.util.List;


public interface CompanyService {

    CompanyResponse createCompany(CompanyRequest request);
    List<CompanyResponse> getAllCompanies();
    CompanyResponse getCompanyById(Long id);

    CompanyResponse updateCompany(Long id, CompanyRequest request);
    void deleteCompany(Long id);
}
