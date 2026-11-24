package com.laptopshop.service;

import com.laptopshop.config.ShopProperties;
import com.laptopshop.exception.BusinessException;
import com.laptopshop.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Pattern SAFE_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final Path root;
    private final long maxBytes;

    public FileStorageService(ShopProperties props) {
        this.root = Paths.get(props.getUpload().getPaymentProofDir()).toAbsolutePath().normalize();
        this.maxBytes = props.getUpload().getMaxProofSizeBytes();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Không tạo được thư mục lưu ảnh minh chứng: " + root, e);
        }
    }

    public String storePaymentProof(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ảnh minh chứng chuyển khoản.");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("Ảnh quá lớn. Dung lượng tối đa " + (maxBytes / 1024 / 1024) + "MB.");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))
                || contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Chỉ chấp nhận ảnh JPG, JPEG, PNG hoặc WEBP.");
        }
        try (InputStream in = file.getInputStream()) {
            String detected = detectExtension(in.readNBytes(12));
            if (detected == null) {
                throw new BusinessException("Tệp tải lên không phải ảnh hợp lệ.");
            }
            String name = UUID.randomUUID() + "." + detected;
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("Tên tệp không hợp lệ.");
            }
            try (InputStream full = file.getInputStream()) {
                Files.copy(full, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return name;
        } catch (IOException e) {
            log.error("[UPLOAD] Lỗi lưu ảnh minh chứng: {}", e.getMessage());
            throw new BusinessException("Không thể lưu ảnh, vui lòng thử lại.");
        }
    }

    public Resource loadAsResource(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh minh chứng.");
        }
        Path path = root.resolve(filename).normalize();
        if (!path.startsWith(root) || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh minh chứng.");
        }
        return new FileSystemResource(path);
    }

    public MediaType mediaTypeOf(String filename) {
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    public void deleteQuietly(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()) return;
        try {
            Files.deleteIfExists(root.resolve(filename).normalize());
        } catch (IOException e) {
            log.warn("[UPLOAD] Không xóa được ảnh cũ {}: {}", filename, e.getMessage());
        }
    }

    private static String detectExtension(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
        return null;
    }
}
