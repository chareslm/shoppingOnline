package com.chareslm.shopping.product.service;

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
 * 商品图片本地存储。文件落在上传根目录的 product/ 前缀下，与资质私有文件隔离。
 */
@Service
public class ProductImageStorage {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", ".jpg", "image/png", ".png");

    private final Path root;

    public ProductImageStorage(MerchantProperties properties) {
        this.root = properties.uploadDir().toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID);
        }
        String detected = detect(file);
        String extension = EXTENSIONS.get(detected);
        if (extension == null) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID);
        }
        String key = "product/" + LocalDate.now() + "/" + UUID.randomUUID() + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(safeOriginalName(file.getOriginalFilename()), key, detected, file.getSize());
        } catch (java.nio.file.AccessDeniedException exception) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID.code(),
                    "无法写入商品图片目录，请检查服务器上传目录权限");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store product image", exception);
        }
    }

    public Resource load(String storageKey) {
        if (storageKey == null || !storageKey.startsWith("product/")) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ignored) {
            // Best-effort cleanup after a failed database insert.
        }
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root) || !key.startsWith("product/")) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID);
        }
        return resolved;
    }

    private String detect(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] prefix = input.readNBytes(8);
            String hex = HexFormat.of().formatHex(prefix);
            if (hex.startsWith("ffd8ff")) {
                return "image/jpeg";
            }
            if (hex.startsWith("89504e470d0a1a0a")) {
                return "image/png";
            }
            return null;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_INVALID);
        }
    }

    private String safeOriginalName(String name) {
        if (name == null || name.isBlank()) {
            return "image";
        }
        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replace("\u0000", "").replace("\r", "").replace("\n", "");
        if (normalized.isBlank()) {
            normalized = "image";
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(normalized.length() - 255);
    }

    public record StoredFile(String originalName, String storageKey, String contentType, long size) {
    }
}
