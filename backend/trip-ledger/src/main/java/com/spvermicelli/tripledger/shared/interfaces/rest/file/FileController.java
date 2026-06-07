package com.spvermicelli.tripledger.shared.interfaces.rest.file;

import com.spvermicelli.tripledger.shared.application.file.FileStorageApplicationService;
import com.spvermicelli.tripledger.shared.application.file.model.StoredFileResult;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import com.spvermicelli.tripledger.shared.interfaces.config.FileStorageProperties;
import com.spvermicelli.tripledger.shared.interfaces.rest.file.response.FileUploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

/**
 * 文件接口。
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageApplicationService fileStorageApplicationService;
    private final FileStorageProperties fileStorageProperties;

    public FileController(
        FileStorageApplicationService fileStorageApplicationService,
        FileStorageProperties fileStorageProperties
    ) {
        this.fileStorageApplicationService = fileStorageApplicationService;
        this.fileStorageProperties = fileStorageProperties;
    }

    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("type") String type,
        @RequestParam(value = "bookId", required = false) Long bookId,
        @RequestParam(value = "billId", required = false) Long billId,
        HttpServletRequest request
    ) {
        ensureBindTokenCanOnlyUploadAvatar(type);
        StoredFileResult result = fileStorageApplicationService.uploadImage(
            UserContextHolder.getUserId(),
            type,
            file,
            bookId,
            billId
        );

        return ApiResponse.success(FileUploadResponse.builder()
            .type(result.getType())
            .fileName(result.getFileName())
            .fileUrl(buildFileUrl(result, request))
            .mimeType(result.getMimeType())
            .fileSize(result.getFileSize())
            .build());
    }

    private void ensureBindTokenCanOnlyUploadAvatar(String type) {
        if (!"BIND_MOBILE".equals(UserContextHolder.getTokenType())) {
            return;
        }
        if (!"avatar".equalsIgnoreCase(String.valueOf(type).trim())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "绑定阶段仅允许上传头像");
        }
    }

    @DeleteMapping("/{type}/{fileName:.+}")
    public ApiResponse<Void> deleteFile(@PathVariable String type, @PathVariable String fileName) {
        fileStorageApplicationService.deleteFile(UserContextHolder.getUserId(), type, fileName);
        return ApiResponse.success();
    }

    @GetMapping("/billing/{fileName:.+}")
    public ResponseEntity<Resource> getBillingImage(@PathVariable String fileName) {
        FileStorageApplicationService.BillingFileView view = fileStorageApplicationService.loadBillingFile(
            UserContextHolder.getUserId(),
            fileName
        );
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(view.mimeType()))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
            .body(view.resource());
    }

    private String buildFileUrl(StoredFileResult result, HttpServletRequest request) {
        String base = fileStorageProperties.resolvePublicBaseUrl();
        if (!StringUtils.hasText(base)) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);
            base = defaultPort
                ? String.format("%s://%s", scheme, serverName)
                : String.format("%s://%s:%d", scheme, serverName, serverPort);
        }
        if ("avatar".equalsIgnoreCase(result.getType())) {
            return base + "/avatar/" + result.getFileName();
        }
        if ("cover".equalsIgnoreCase(result.getType())) {
            return base + "/cover/" + result.getFileName();
        }
        return base + "/api/v1/files/billing/" + result.getFileName();
    }
}
