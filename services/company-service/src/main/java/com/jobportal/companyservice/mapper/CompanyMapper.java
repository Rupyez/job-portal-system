package com.jobportal.companyservice.mapper;

import com.jobportal.companyservice.dto.CompanyRequest;
import com.jobportal.companyservice.dto.CompanyResponse;
import com.jobportal.companyservice.entity.Company;

public class CompanyMapper {

    public Company toEntity(CompanyRequest request){
        Company company = new Company();
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setIndustry(request.getIndustry());
        company.setCompanySize(request.getCompanySize());
        company.setLocation(request.getLocation());

        return company;
    }

    public void updateEntity(Company company, CompanyRequest request){
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setIndustry(request.getIndustry());
        company.setCompanySize(request.getCompanySize());
        company.setLocation(request.getLocation());
    }

    public CompanyResponse toResponse(Company company){
        CompanyResponse response = new CompanyResponse();

        response.setId(company.getId());
        response.setName(company.getName());
        response.setDescription(company.getDescription());
        response.setWebsite(company.getWebsite());
        response.setIndustry(company.getIndustry());
        response.setCompanySize(company.getCompanySize());
        response.setLocation(company.getLocation());
        response.setCreatedAt(company.getCreatedAt());
        response.setUpdatedAt(company.getUpdatedAt());

        return response;
    }
}
