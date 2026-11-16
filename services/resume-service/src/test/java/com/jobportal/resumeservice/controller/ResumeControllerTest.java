package com.jobportal.resumeservice.controller;

import com.jobportal.resumeservice.dto.request.ResumeUpdateRequest;
import com.jobportal.resumeservice.dto.request.ResumeUploadRequest;
import com.jobportal.resumeservice.dto.response.ResumeResponse;
import com.jobportal.resumeservice.security.AppUserPrincipal;
import com.jobportal.resumeservice.service.ResumeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeControllerTest {

    private static final Long USER_ID = 100L;
    private static final UUID RESUME_ID = UUID.fromString(
            "3b169ae1-5a3c-4c85-865a-f17fc3c7f672");

    private MockMvc mockMvc;
    private ResumeService resumeService;
    private AppUserPrincipal principal;

    @BeforeEach
    void setUp() {
        resumeService = mock(ResumeService.class);

        principal = new AppUserPrincipal(
                USER_ID,
                "rupesh@example.com",
                "Rupesh",
                Map.of()
        );

        ResumeController controller = new ResumeController(resumeService);

        // Resolver that hands the principal to every AppUserPrincipal parameter.
        // No Spring Security filter chain involved — deterministic every time.
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return AppUserPrincipal.class.equals(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    // ------------------------------------------------------------------
    // Test data
    // ------------------------------------------------------------------

    private ResumeResponse sampleResponse() {
        Instant now = Instant.now();
        return new ResumeResponse(
                RESUME_ID,
                USER_ID,
                "My Resume",
                "Java Developer",
                "resume.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                1024L,
                now,
                now,
                0L
        );
    }

    // ------------------------------------------------------------------
    // Upload
    // ------------------------------------------------------------------
    @Test
    void shouldUploadResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE,
                "Resume content".getBytes());

        when(resumeService.upload(eq(USER_ID), any(ResumeUploadRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(multipart("/")
                        .file(file)
                        .param("title", "My Resume")
                        .param("summary", "Java Developer"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Resume"))
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.originalFileName").value("resume.pdf"));

        verify(resumeService).upload(eq(USER_ID), any(ResumeUploadRequest.class));
    }

    // ------------------------------------------------------------------
    // List
    // ------------------------------------------------------------------
    @Test
    void shouldListResumes() throws Exception {
        when(resumeService.list(USER_ID))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("My Resume"))
                .andExpect(jsonPath("$[0].userId").value(100))
                .andExpect(jsonPath("$[0].originalFileName").value("resume.pdf"));

        verify(resumeService).list(USER_ID);
    }

    // ------------------------------------------------------------------
    // Get one
    // ------------------------------------------------------------------
    @Test
    void shouldGetResumeById() throws Exception {
        when(resumeService.get(USER_ID, RESUME_ID))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/{id}", RESUME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Resume"))
                .andExpect(jsonPath("$.userId").value(100))
                .andExpect(jsonPath("$.originalFileName").value("resume.pdf"));

        verify(resumeService).get(USER_ID, RESUME_ID);
    }

    // ------------------------------------------------------------------
    // Update metadata
    // ------------------------------------------------------------------
    @Test
    void shouldUpdateResumeMetadata() throws Exception {
        String body = """
                {
                  "title": "Updated Resume",
                  "summary": "Senior Java Developer"
                }
                """;

        when(resumeService.updateMetaData(
                eq(USER_ID), eq(RESUME_ID), any(ResumeUpdateRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(patch("/{id}", RESUME_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Resume"));

        verify(resumeService).updateMetaData(
                eq(USER_ID), eq(RESUME_ID), any(ResumeUpdateRequest.class));
    }

    // ------------------------------------------------------------------
    // Replace file
    // ------------------------------------------------------------------
    @Test
    void shouldReplaceResumeFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "updated-resume.pdf", MediaType.APPLICATION_PDF_VALUE,
                "Updated resume content".getBytes());

        when(resumeService.replaceFile(eq(USER_ID), eq(RESUME_ID), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(multipart(HttpMethod.PUT, "/{id}/file", RESUME_ID)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Resume"));

        verify(resumeService).replaceFile(eq(USER_ID), eq(RESUME_ID), any());
    }

    // ------------------------------------------------------------------
    // Download
    // ------------------------------------------------------------------
    @Test
    void shouldDownloadResume() throws Exception {
        byte[] bytes = "Resume file content".getBytes();
        ByteArrayResource resource = new ByteArrayResource(bytes);

        when(resumeService.get(USER_ID, RESUME_ID)).thenReturn(sampleResponse());
        when(resumeService.download(USER_ID, RESUME_ID)).thenReturn(resource);

        mockMvc.perform(get("/{id}/download", RESUME_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("resume.pdf")))
                .andExpect(content().bytes(bytes));

        verify(resumeService).get(USER_ID, RESUME_ID);
        verify(resumeService).download(USER_ID, RESUME_ID);
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------
    @Test
    void shouldDeleteResume() throws Exception {
        doNothing().when(resumeService).delete(USER_ID, RESUME_ID);

        mockMvc.perform(delete("/{id}", RESUME_ID))
                .andExpect(status().isNoContent());

        verify(resumeService).delete(USER_ID, RESUME_ID);
    }
}