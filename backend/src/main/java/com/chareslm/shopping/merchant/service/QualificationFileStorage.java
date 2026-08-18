package com.chareslm.shopping.merchant.service;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.config.MerchantProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * 商家资质文件的私有本地存储。
 *
 * <p>文件类型依据内容签名而非客户端声明识别，所有存储键都在规范化后约束于配置根目录内。</p>
 */
@Service
public class QualificationFileStorage {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");

    private final Path root;

    public QualificationFileStorage(MerchantProperties properties) {
        this.root = properties.uploadDir().toAbsolutePath().normalize();
    }

    /**
     * 校验并保存文件，返回持久化元数据所需的不可预测存储键。
     */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.MERCHANT_FILE_INVALID);
        }
        String detected = detect(file);
        String extension = EXTENSIONS.get(detected);
        if (extension == null) {
            throw new BusinessException(ErrorCode.MERCHANT_FILE_INVALID);
        }
        String key = LocalDate.now() + "/" + UUID.randomUUID() + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(safeOriginalName(file.getOriginalFilename()), key, detected, file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store qualification file", exception);
        }
    }

    /**
     * 加载私有文件；不存在、不可读或非法存储键统一按资源不存在处理。
     */
    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    /**
     * 尽力清理孤立文件，清理失败不会覆盖触发补偿的原始业务异常。
     */
    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ignored) {
            // Best-effort cleanup after a failed database transaction.
        }
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        // normalize 后再次校验根目录，拒绝 ../、绝对路径等路径穿越输入。
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.MERCHANT_FILE_INVALID);
        }
        return resolved;
    }

    private String detect(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            // 不采信 Content-Type 或扩展名，只接受白名单格式的 magic bytes。
            byte[] prefix = input.readNBytes(8);
            String hex = HexFormat.of().formatHex(prefix);
            if (hex.startsWith("25504446")) return "application/pdf";
            if (hex.startsWith("ffd8ff")) return "image/jpeg";
            if (hex.startsWith("89504e470d0a1a0a")) return "image/png";
            return null;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.MERCHANT_FILE_INVALID);
        }
    }

    private String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "qualification";
        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replace("\u0000", "").replace("\r", "").replace("\n", "");
        if (normalized.isBlank()) normalized = "qualification";
        return normalized.length() <= 255 ? normalized : normalized.substring(normalized.length() - 255);
    }

    public record StoredFile(String originalName, String storageKey, String contentType, long size) {
    }
}
