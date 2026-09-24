package com.fintrack.document.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintrack.document.client.incident.IncidentServiceClientService;
import com.fintrack.document.config.SecurityConfig;
import com.fintrack.document.model.dto.request.AttachmentMetadataRequest;
import com.fintrack.document.model.dto.response.AttachmentMetadataResponse;
import com.fintrack.document.model.dto.response.IncidentSummaryResponse;
import com.fintrack.document.model.dto.response.UserSummaryResponse;
import com.fintrack.document.model.entity.AttachmentMetadata;
import com.fintrack.document.model.mapper.AttachmentMetadataMapper;
import com.fintrack.document.security.JwtUtils;
import com.fintrack.document.service.AttachmentMetadataService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttachmentMetadataController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "testuser", authorities = { "INCIDENT_VIEW_ALL" })
class AttachmentMetadataControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(
    new JavaTimeModule()
  );

  @MockitoBean
  private AttachmentMetadataService attachmentMetadataService;

  @MockitoBean
  private AttachmentMetadataMapper attachmentMetadataMapper;

  @MockitoBean
  private IncidentServiceClientService incidentServiceClientService;

  @MockitoBean
  private JwtUtils jwtUtils;

  private AttachmentMetadata testAttachment;
  private AttachmentMetadataResponse testResponse;
  private UUID testId;
  private UUID incidentId;
  private UUID uploadedBy;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    incidentId = UUID.randomUUID();
    uploadedBy = UUID.randomUUID();

    testAttachment = new AttachmentMetadata();
    testAttachment.setId(testId);
    testAttachment.setIncidentId(incidentId);
    testAttachment.setFilename("test.pdf");
    testAttachment.setStoragePath("/uploads/test.pdf");
    testAttachment.setFileSize(1024L);
    testAttachment.setMimeType("application/pdf");
    testAttachment.setUploadedBy(uploadedBy);
    testAttachment.setUploadedAt(LocalDateTime.now());

    testResponse = AttachmentMetadataResponse.builder()
      .incidentId(incidentId)
      .incident(
        IncidentSummaryResponse.builder()
          .id(incidentId)
          .title("Test Incident")
          .status("OPEN")
          .build()
      )
      .filename("test.pdf")
      .storagePath("/uploads/test.pdf")
      .fileSize(1024L)
      .mimeType("application/pdf")
      .uploadedById(uploadedBy)
      .uploadedBy(
        UserSummaryResponse.builder().id(uploadedBy).username("jdoe").build()
      )
      .uploadedAt(LocalDateTime.now())
      .build();
    testResponse.setId(testId);
  }

  @Test
  @DisplayName(
    "GET /api/v1/documentService/attachments - Returns page of attachments"
  )
  void getAllAttachments_ReturnsPage() throws Exception {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<AttachmentMetadata> page = new PageImpl<>(
      List.of(testAttachment),
      pageable,
      1
    );

    when(attachmentMetadataService.findAll(any(PageRequest.class))).thenReturn(
      page
    );
    when(
      attachmentMetadataMapper.toResponse(any(AttachmentMetadata.class))
    ).thenReturn(testResponse);

    mockMvc
      .perform(
        get("/api/v1/documentService/attachments")
          .param("page", "0")
          .param("size", "10")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].filename").value("test.pdf"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/documentService/attachments/all - Returns list of attachments"
  )
  void getAllAttachmentsList_ReturnsList() throws Exception {
    when(attachmentMetadataService.findAll()).thenReturn(
      List.of(testAttachment)
    );
    when(
      attachmentMetadataMapper.toResponse(any(AttachmentMetadata.class))
    ).thenReturn(testResponse);

    mockMvc
      .perform(get("/api/v1/documentService/attachments/all"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].filename").value("test.pdf"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/documentService/attachments/{id} - Returns attachment by ID"
  )
  void getAttachmentById_ReturnsAttachment() throws Exception {
    when(attachmentMetadataService.findById(testId)).thenReturn(testAttachment);
    when(
      attachmentMetadataMapper.toResponse(any(AttachmentMetadata.class))
    ).thenReturn(testResponse);

    mockMvc
      .perform(get("/api/v1/documentService/attachments/{id}", testId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.filename").value("test.pdf"))
      .andExpect(jsonPath("$.incidentId").value(incidentId.toString()))
      .andExpect(jsonPath("$.incident.title").value("Test Incident"))
      .andExpect(jsonPath("$.uploadedById").value(uploadedBy.toString()))
      .andExpect(jsonPath("$.uploadedBy.username").value("jdoe"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/documentService/attachments/incident/{incidentId} - Returns attachments by incident"
  )
  void getAttachmentsByIncidentId_ReturnsAttachments() throws Exception {
    when(attachmentMetadataService.findByIncidentId(incidentId)).thenReturn(
      List.of(testAttachment)
    );
    when(
      attachmentMetadataMapper.toResponse(any(AttachmentMetadata.class))
    ).thenReturn(testResponse);

    mockMvc
      .perform(
        get(
          "/api/v1/documentService/attachments/incident/{incidentId}",
          incidentId
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].filename").value("test.pdf"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/documentService/attachments/uploaded-by/{uploadedBy} - Returns attachments by uploader"
  )
  void getAttachmentsByUploadedBy_ReturnsAttachments() throws Exception {
    when(jwtUtils.getUserIdFromJwtToken("tok")).thenReturn(
      uploadedBy.toString()
    );
    when(attachmentMetadataService.findByUploadedBy(uploadedBy)).thenReturn(
      List.of(testAttachment)
    );
    when(
      attachmentMetadataMapper.toResponse(any(AttachmentMetadata.class))
    ).thenReturn(testResponse);

    mockMvc
      .perform(
        get(
          "/api/v1/documentService/attachments/uploaded-by/{uploadedBy}",
          uploadedBy
        ).header("Authorization", "Bearer tok")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].filename").value("test.pdf"));
  }

  @Test
  @DisplayName(
    "POST /api/v1/documentService/attachments - Refuses raw metadata"
  )
  void createAttachment_ReturnsCreated() throws Exception {
    AttachmentMetadataRequest request = new AttachmentMetadataRequest();
    request.setIncidentId(incidentId);
    request.setFilename("forged.pdf");
    request.setStoragePath("other-user/secret.pdf");
    request.setFileSize(1L);
    request.setMimeType("application/pdf");
    request.setUploadedBy(uploadedBy);
    request.setUploadedAt(LocalDateTime.now());
    mockMvc.perform(post("/api/v1/documentService/attachments")
      .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isForbidden());
    org.mockito.Mockito.verifyNoInteractions(attachmentMetadataService);
  }

  @Test
  @DisplayName(
    "PUT /api/v1/documentService/attachments/{id} - Refuses raw metadata"
  )
  void updateAttachment_ReturnsUpdated() throws Exception {
    AttachmentMetadataRequest request = new AttachmentMetadataRequest();
    request.setIncidentId(incidentId);
    request.setFilename("forged.pdf");
    request.setStoragePath("other-user/secret.pdf");
    request.setFileSize(1L);
    request.setMimeType("application/pdf");
    request.setUploadedBy(uploadedBy);
    request.setUploadedAt(LocalDateTime.now());
    mockMvc.perform(put("/api/v1/documentService/attachments/{id}", testId)
      .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isForbidden());
    org.mockito.Mockito.verifyNoInteractions(attachmentMetadataService);
  }

  @Test
  @DisplayName(
    "DELETE /api/v1/documentService/attachments/{id} - Deletes attachment and returns 204"
  )
  void deleteAttachment_ReturnsNoContent() throws Exception {
    when(attachmentMetadataService.findById(testId)).thenReturn(testAttachment);
    when(jwtUtils.getUserIdFromJwtToken("tok")).thenReturn(
      uploadedBy.toString()
    );
    doNothing()
      .when(incidentServiceClientService)
      .assertAttachmentAllowed(incidentId, "INCIDENT", null);
    doNothing().when(attachmentMetadataService).delete(testId);

    mockMvc
      .perform(
        delete("/api/v1/documentService/attachments/{id}", testId)
          .header("Authorization", "Bearer tok")
          .with(csrf())
      )
      .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName(
    "POST /upload with category=CANCELLATION checks the cancellation action"
  )
  void uploadAttachment_Cancellation_UsesResolutionAccessCheck() throws Exception {
    UUID uploadId = UUID.randomUUID();
    when(jwtUtils.getUserIdFromJwtToken("tok")).thenReturn(
      uploadedBy.toString()
    );
    when(attachmentMetadataMapper.toUpload(any(), any(), any(), any(), any()))
      .thenReturn(new com.fintrack.document.model.entity.AttachmentUpload());
    when(attachmentMetadataService.upload(any())).thenReturn(testAttachment);
    when(
      attachmentMetadataMapper.toResponse(any(AttachmentMetadata.class))
    ).thenReturn(testResponse);

    MockMultipartFile file = new MockMultipartFile(
      "file",
      "reason.pdf",
      "application/pdf",
      "content".getBytes()
    );

    mockMvc
      .perform(
        multipart("/api/v1/documentService/attachments/upload")
          .file(file)
          .param("incidentId", incidentId.toString())
          .param("category", "CANCELLATION")
          .param("uploadId", uploadId.toString())
          .header("Authorization", "Bearer tok")
          .with(csrf())
      )
      .andExpect(status().isCreated());

    verify(incidentServiceClientService).assertAttachmentAllowed(incidentId, "CANCELLATION", null);
    verify(attachmentMetadataService).upload(org.mockito.ArgumentMatchers.argThat(upload -> uploadId.equals(upload.getUploadId())));
  }

  @Test
  void legacyActiveContentIsDownloadedAsAnInertAttachment() throws Exception {
    testResponse.setMimeType("text/html");
    when(attachmentMetadataService.findById(testId)).thenReturn(testAttachment);
    when(attachmentMetadataMapper.toResponse(testAttachment)).thenReturn(testResponse);
    when(attachmentMetadataService.download(testId)).thenReturn(new org.springframework.core.io.ByteArrayResource("<html>bad</html>".getBytes()));
    mockMvc.perform(get("/api/v1/documentService/attachments/{id}/download", testId))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
      .andExpect(header().string("X-Content-Type-Options", "nosniff"))
      .andExpect(header().string("Content-Security-Policy", "sandbox; default-src 'none'"));
    verify(incidentServiceClientService).assertIncidentAccessible(incidentId);
  }

  @Test
  @DisplayName(
    "DELETE /api/v1/documentService/attachments/incident/{incidentId} - Refuses public bulk deletion"
  )
  void deleteAttachmentsByIncidentId_ReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/documentService/attachments/incident/{incidentId}", incidentId))
      .andExpect(status().isForbidden());
    org.mockito.Mockito.verifyNoInteractions(attachmentMetadataService);
  }
}
