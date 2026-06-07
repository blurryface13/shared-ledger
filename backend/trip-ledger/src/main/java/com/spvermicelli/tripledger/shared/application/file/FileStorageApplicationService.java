package com.spvermicelli.tripledger.shared.application.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import com.spvermicelli.tripledger.shared.application.file.model.FileAssetMetadata;
import com.spvermicelli.tripledger.shared.application.file.model.StoredFileResult;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.interfaces.config.FileStorageProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储应用服务。
 * 统一处理头像、账本封面、账单图片的上传、读取和删除链路。
 */
@Service
public class FileStorageApplicationService {

    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[a-f0-9]{32}\\.(jpg|png)$");
    private static final String METADATA_FILE_SUFFIX = ".meta.json";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "heif", "heic");
    private static final int BILLING_FILE_LIMIT_PER_BILL = 5;
    private static final byte[] JPEG_MAGIC = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] RIFF_MAGIC = new byte[] {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = new byte[] {0x57, 0x45, 0x42, 0x50};
    private static final Set<String> HEIF_FILETYPE_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1");
    private static final Set<PosixFilePermission> SAFE_FILE_PERMISSIONS = EnumSet.of(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.GROUP_READ
    );

    private final FileStorageProperties fileStorageProperties;
    private final ObjectMapper objectMapper;
    private final BillingAccessSupportService billingAccessSupportService;

    public FileStorageApplicationService(
        FileStorageProperties fileStorageProperties,
        ObjectMapper objectMapper,
        BillingAccessSupportService billingAccessSupportService
    ) {
        this.fileStorageProperties = fileStorageProperties;
        this.objectMapper = objectMapper;
        this.billingAccessSupportService = billingAccessSupportService;
    }

    @Transactional
    public StoredFileResult uploadImage(
        Long currentUserId,
        String typeCode,
        MultipartFile file,
        Long bookId,
        Long billId
    ) {
        ensureAuthenticated(currentUserId);
        UploadType type = UploadType.fromCode(typeCode);
        validateBillingScope(currentUserId, type, bookId, billId);
        validateMultipart(file);

        byte[] originalBytes = readMultipartBytes(file);
        if (originalBytes.length > fileStorageProperties.getMaxUploadBytes()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "上传文件大小不能超过 10MB");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "";
        String extension = normalizeExtension(originalName);
        if (StringUtils.hasText(extension) && !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "图片格式仅支持 jpg/jpeg/png/webp/heif/heic");
        }

        ImageFormat magicFormat = detectFormatByMagic(originalBytes);
        if (magicFormat == null) {
            throw new BusinessException(
                ErrorCode.INVALID_PARAM,
                "仅支持常见图片格式（jpg/jpeg/png/webp/heif/heic），且文件头必须合法"
            );
        }
        if (!StringUtils.hasText(extension) || !magicFormat.matchesExtension(extension)) {
            // 跨端上传时后缀可能不可靠，以文件头识别结果作为最终格式。
            extension = magicFormat.getExtension();
        }

        BufferedImage originalImage = readImage(originalBytes);
        if (originalImage == null || originalImage.getWidth() <= 0 || originalImage.getHeight() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "图片内容解析失败，请上传有效图片");
        }

        long targetBytes = targetBytesByType(type);
        CompressedImage compressedImage = compressImage(originalImage, magicFormat, targetBytes);
        if (compressedImage.bytes().length > targetBytes) {
            throw new BusinessException(
                ErrorCode.INVALID_PARAM,
                type == UploadType.BILLING ? "账单图片压缩后必须小于等于 3MB" : "头像/封面压缩后必须小于等于 1MB"
            );
        }

        Path directory = directoryByType(type);
        Path metadataDirectory = metadataDirectoryByType(type);
        ensureDirectory(directory);
        ensureDirectory(metadataDirectory);
        validateUploadCountLimit(currentUserId, type, billId, directory, metadataDirectory);

        for (int attempt = 0; attempt < 5; attempt++) {
            String safeFileName = generateSafeFileName(originalName, compressedImage.format());
            Path filePath = directory.resolve(safeFileName).normalize();
            Path metadataPath = metadataPath(metadataDirectory, safeFileName).normalize();
            ensureWithinDirectory(directory, filePath);
            ensureWithinDirectory(metadataDirectory, metadataPath);

            try {
                Files.write(filePath, compressedImage.bytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                hardenFilePermission(filePath);

                FileAssetMetadata metadata = new FileAssetMetadata();
                metadata.setType(type.getCode());
                metadata.setFileName(safeFileName);
                metadata.setOriginalName(originalName);
                metadata.setMimeType(compressedImage.mimeType());
                metadata.setFileSize(compressedImage.bytes().length);
                metadata.setUploaderUserId(currentUserId);
                metadata.setBookId(bookId);
                metadata.setBillId(billId);
                metadata.setUploadedAt(LocalDateTime.now());

                Files.writeString(
                    metadataPath,
                    objectMapper.writeValueAsString(metadata),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
                );
                hardenFilePermission(metadataPath);

                return StoredFileResult.builder()
                    .type(type.getCode())
                    .fileName(safeFileName)
                    .mimeType(compressedImage.mimeType())
                    .fileSize(compressedImage.bytes().length)
                    .build();
            } catch (FileAlreadyExistsException ignore) {
                // 极低概率命中同名时重试。
            } catch (IOException exception) {
                safeDelete(filePath);
                safeDelete(metadataPath);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败，请稍后重试");
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成唯一文件名失败，请稍后重试");
    }

    @Transactional
    public void deleteFile(Long currentUserId, String typeCode, String fileName) {
        ensureAuthenticated(currentUserId);
        UploadType type = UploadType.fromCode(typeCode);
        validateSafeFileName(fileName);

        Path directory = directoryByType(type);
        Path metadataDirectory = metadataDirectoryByType(type);
        Path filePath = directory.resolve(fileName).normalize();
        Path metadataPath = resolveMetadataPath(directory, metadataDirectory, fileName);
        ensureWithinDirectory(directory, filePath);
        if (metadataPath.startsWith(metadataDirectory)) {
            ensureWithinDirectory(metadataDirectory, metadataPath);
        } else {
            ensureWithinDirectory(directory, metadataPath);
        }

        FileAssetMetadata metadata = readMetadata(metadataPath);
        if (!Objects.equals(metadata.getUploaderUserId(), currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅上传者本人可以删除该图片");
        }

        safeDelete(filePath);
        safeDelete(metadataPath);
    }

    @Transactional(readOnly = true)
    public BillingFileView loadBillingFile(Long currentUserId, String fileName) {
        ensureAuthenticated(currentUserId);
        validateSafeFileName(fileName);

        Path directory = fileStorageProperties.resolveBillingDir();
        Path metadataDirectory = metadataDirectoryByType(UploadType.BILLING);
        Path filePath = directory.resolve(fileName).normalize();
        Path metadataPath = resolveMetadataPath(directory, metadataDirectory, fileName);
        ensureWithinDirectory(directory, filePath);
        if (metadataPath.startsWith(metadataDirectory)) {
            ensureWithinDirectory(metadataDirectory, metadataPath);
        } else {
            ensureWithinDirectory(directory, metadataPath);
        }

        FileAssetMetadata metadata = readMetadata(metadataPath);
        if (!UploadType.BILLING.getCode().equals(metadata.getType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        validateBillingReadPermission(currentUserId, metadata);
        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        return new BillingFileView(new FileSystemResource(filePath), resolveMimeType(metadata.getMimeType()));
    }

    private void validateBillingScope(Long currentUserId, UploadType type, Long bookId, Long billId) {
        if (type != UploadType.BILLING) {
            return;
        }
        if (bookId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "上传账单图片时 bookId 不能为空");
        }
        billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        if (billId != null) {
            billingAccessSupportService.requireVisibleBill(bookId, billId, currentUserId);
        }
    }

    private void validateBillingReadPermission(Long currentUserId, FileAssetMetadata metadata) {
        if (metadata.getBookId() == null) {
            if (!Objects.equals(metadata.getUploaderUserId(), currentUserId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权访问该账单图片");
            }
            return;
        }
        if (metadata.getBillId() != null) {
            billingAccessSupportService.requireVisibleBill(metadata.getBookId(), metadata.getBillId(), currentUserId);
            return;
        }
        billingAccessSupportService.requireActiveContext(currentUserId, metadata.getBookId());
    }

    private void validateMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "上传文件不能为空");
        }
    }

    private byte[] readMultipartBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        }
    }

    private BufferedImage readImage(byte[] bytes) {
        try {
            return javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException exception) {
            return null;
        }
    }

    private CompressedImage compressImage(BufferedImage image, ImageFormat sourceFormat, long targetBytes) {
        List<ImageFormat> candidates = sourceFormat == ImageFormat.PNG
            ? List.of(ImageFormat.PNG, ImageFormat.JPEG)
            : (sourceFormat == ImageFormat.WEBP || sourceFormat == ImageFormat.HEIF)
                ? List.of(ImageFormat.JPEG)
            : List.of(ImageFormat.JPEG);

        for (ImageFormat candidate : candidates) {
            double scale = 1.0d;
            double quality = candidate == ImageFormat.JPEG ? 0.92d : 1.0d;
            BufferedImage workingImage = candidate == ImageFormat.JPEG ? toJpegSafeImage(image) : image;

            for (int round = 0; round < 12; round++) {
                byte[] bytes = renderImage(workingImage, candidate, scale, quality);
                if (bytes.length <= targetBytes) {
                    return new CompressedImage(bytes, candidate.getExtension(), candidate.getMimeType());
                }
                scale = Math.max(scale * 0.88d, 0.20d);
                quality = Math.max(quality * 0.86d, 0.40d);
            }
        }

        byte[] fallback = renderImage(toJpegSafeImage(image), ImageFormat.JPEG, 0.20d, 0.40d);
        return new CompressedImage(fallback, ImageFormat.JPEG.getExtension(), ImageFormat.JPEG.getMimeType());
    }

    private byte[] renderImage(BufferedImage image, ImageFormat format, double scale, double quality) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var builder = Thumbnails.of(image)
                .scale(scale)
                .outputFormat(format.getExtension());
            if (format == ImageFormat.JPEG) {
                builder.outputQuality(quality);
            }
            builder.toOutputStream(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }

    private BufferedImage toJpegSafeImage(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) {
            return source;
        }
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return target;
    }

    private ImageFormat detectFormatByMagic(byte[] bytes) {
        if (startsWith(bytes, JPEG_MAGIC)) {
            return ImageFormat.JPEG;
        }
        if (startsWith(bytes, PNG_MAGIC)) {
            return ImageFormat.PNG;
        }
        if (isWebp(bytes)) {
            return ImageFormat.WEBP;
        }
        if (isHeif(bytes)) {
            return ImageFormat.HEIF;
        }
        return null;
    }

    private boolean isWebp(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }
        if (!startsWith(bytes, RIFF_MAGIC)) {
            return false;
        }
        for (int index = 0; index < WEBP_MAGIC.length; index++) {
            if (bytes[8 + index] != WEBP_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isHeif(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }
        if (bytes[4] != 0x66 || bytes[5] != 0x74 || bytes[6] != 0x79 || bytes[7] != 0x70) {
            return false;
        }
        String brand = new String(bytes, 8, 4, StandardCharsets.US_ASCII).toLowerCase();
        return HEIF_FILETYPE_BRANDS.contains(brand);
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes == null || prefix == null || bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String normalizeExtension(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return null;
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    private String generateSafeFileName(String originalName, String extension) {
        String normalizedOriginal = StringUtils.hasText(originalName) ? originalName.trim() : "image";
        String base = normalizedOriginal.contains(".")
            ? normalizedOriginal.substring(0, normalizedOriginal.lastIndexOf('.'))
            : normalizedOriginal;
        String compactBase = base.replaceAll("[^a-zA-Z0-9_-]", "");
        if (!StringUtils.hasText(compactBase)) {
            compactBase = "image";
        }
        String seed = System.currentTimeMillis() + ":" + compactBase + ":" + UUID.randomUUID();
        String hash = sha256Hex(seed).substring(0, 32).toLowerCase();
        return hash + "." + extension;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成文件名失败");
        }
    }

    private void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化文件目录失败");
        }
    }

    private void hardenFilePermission(Path path) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path, SAFE_FILE_PERMISSIONS);
            }
        } catch (Exception ignored) {
            // 非 POSIX 文件系统或权限不支持时忽略，默认权限同样不会赋予执行位。
        }
    }

    private Path metadataPath(Path metadataDirectory, String fileName) {
        return metadataDirectory.resolve(fileName + ".meta.json");
    }

    private Path resolveMetadataPath(Path fileDirectory, Path metadataDirectory, String fileName) {
        Path preferred = metadataPath(metadataDirectory, fileName).normalize();
        if (Files.exists(preferred)) {
            return preferred;
        }
        Path legacy = metadataPath(fileDirectory, fileName).normalize();
        if (Files.exists(legacy)) {
            return legacy;
        }
        return preferred;
    }

    private void validateUploadCountLimit(
        Long currentUserId,
        UploadType type,
        Long billId,
        Path fileDirectory,
        Path metadataDirectory
    ) {
        if (type != UploadType.BILLING || billId == null) {
            return;
        }

        long uploadedCount = countBillingFiles(currentUserId, billId, fileDirectory, metadataDirectory);
        if (uploadedCount >= BILLING_FILE_LIMIT_PER_BILL) {
            throw new BusinessException(
                ErrorCode.INVALID_PARAM,
                "单条账单最多上传 5 张图片，请先删除后再上传"
            );
        }
    }

    private long countBillingFiles(Long currentUserId, Long billId, Path fileDirectory, Path metadataDirectory) {
        Set<Path> metadataPaths = new HashSet<>();
        collectMetadataPaths(fileDirectory, metadataPaths);
        collectMetadataPaths(metadataDirectory, metadataPaths);
        return metadataPaths.stream()
            .map(this::readMetadataQuietly)
            .filter(Objects::nonNull)
            .filter(item -> UploadType.BILLING.getCode().equals(item.getType()))
            .filter(item -> Objects.equals(currentUserId, item.getUploaderUserId()))
            .filter(item -> Objects.equals(billId, item.getBillId()))
            .count();
    }

    private void collectMetadataPaths(Path directory, Set<Path> collector) {
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(METADATA_FILE_SUFFIX))
                .map(Path::normalize)
                .forEach(collector::add);
        } catch (IOException ignored) {
            // 元数据读取失败时不影响主流程，保持业务可用性。
        }
    }

    private FileAssetMetadata readMetadataQuietly(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), FileAssetMetadata.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private FileAssetMetadata readMetadata(Path metadataPath) {
        if (!Files.exists(metadataPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        try {
            return objectMapper.readValue(metadataPath.toFile(), FileAssetMetadata.class);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取图片元数据失败");
        }
    }

    private void validateSafeFileName(String fileName) {
        if (!StringUtils.hasText(fileName) || !SAFE_FILE_NAME.matcher(fileName).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "文件名非法");
        }
    }

    private void ensureWithinDirectory(Path directory, Path targetPath) {
        if (!targetPath.startsWith(directory.normalize())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "文件路径非法");
        }
    }

    private void safeDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 删除失败不抛出，避免误伤主流程。
        }
    }

    private Path directoryByType(UploadType type) {
        return switch (type) {
            case AVATAR -> fileStorageProperties.resolveAvatarDir();
            case COVER -> fileStorageProperties.resolveCoverDir();
            case BILLING -> fileStorageProperties.resolveBillingDir();
        };
    }

    private Path metadataDirectoryByType(UploadType type) {
        return fileStorageProperties.resolveBaseDir()
            .resolve(".meta")
            .resolve(type.getCode())
            .normalize();
    }

    private long targetBytesByType(UploadType type) {
        return switch (type) {
            case AVATAR -> fileStorageProperties.getAvatarTargetBytes();
            case COVER -> fileStorageProperties.getCoverTargetBytes();
            case BILLING -> fileStorageProperties.getBillingTargetBytes();
        };
    }

    private String resolveMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return "application/octet-stream";
        }
        return mimeType;
    }

    private void ensureAuthenticated(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
    }

    public record BillingFileView(Resource resource, String mimeType) {
    }

    private record CompressedImage(byte[] bytes, String format, String mimeType) {
    }

    private enum UploadType {
        AVATAR("avatar"),
        COVER("cover"),
        BILLING("billing");

        private final String code;

        UploadType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static UploadType fromCode(String code) {
            if (!StringUtils.hasText(code)) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "type 不能为空");
            }
            for (UploadType type : values()) {
                if (type.code.equalsIgnoreCase(code.trim())) {
                    return type;
                }
            }
            throw new BusinessException(ErrorCode.INVALID_PARAM, "type 仅支持 avatar、cover、billing");
        }
    }

    private enum ImageFormat {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp"),
        HEIF("heic", "image/heic");

        private final String extension;
        private final String mimeType;

        ImageFormat(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public boolean matchesExtension(String extension) {
            if (!StringUtils.hasText(extension)) {
                return false;
            }
            String normalized = extension.toLowerCase();
            if (this == JPEG) {
                return "jpg".equals(normalized) || "jpeg".equals(normalized);
            }
            if (this == HEIF) {
                return "heif".equals(normalized) || "heic".equals(normalized);
            }
            return this.extension.equals(normalized);
        }
    }
}
