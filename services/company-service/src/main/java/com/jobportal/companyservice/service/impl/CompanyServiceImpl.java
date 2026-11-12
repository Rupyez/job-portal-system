package com.jobportal.companyservice.service.impl;

import com.jobportal.companyservice.dto.CompanyRequest;
import com.jobportal.companyservice.dto.CompanyResponse;
import com.jobportal.companyservice.entity.Company;
import com.jobportal.companyservice.exception.CompanyNotFoundException;
import com.jobportal.companyservice.mapper.CompanyMapper;
import com.jobportal.companyservice.repository.CompanyRepository;
import com.jobportal.companyservice.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyResponse createCompany(CompanyRequest request) {
        Company company = companyMapper.toEntity(request);

        Company savedCompany = companyRepository.save(company);
        return companyMapper.toResponse(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {

        return companyRepository.findAll()
                .stream()
                .map(companyMapper::toResponse)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = findCompanyById(id);
        return companyMapper.toResponse(company);
    }

    private Company findCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }

    @Override
    public CompanyResponse updateCompany(Long id, CompanyRequest request) {

        Company company = findCompanyById(id);
        companyMapper.updateEntity(company, request);

        Company updatedCompany = companyRepository.save(company);
        return companyMapper.toResponse(updatedCompany);
    }

    @Override
    public void deleteCompany(Long id) {
        Company company = findCompanyById(id);
        companyRepository.delete(company);
    }


}
