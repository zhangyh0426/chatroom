package com.chatroom.tieba.support;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ThreadImagePolicy {

    public static final int MAX_IMAGE_COUNT = 9;
    public static final int MAX_SINGLE_FILE_SIZE_MB = 5;
    public static final long MAX_SINGLE_FILE_SIZE = MAX_SINGLE_FILE_SIZE_MB * 1024L * 1024L;
    public static final String UPLOAD_SUB_DIRECTORY = "thread-images";

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    private ThreadImagePolicy() {
    }

    public static List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    public static String acceptAttribute() {
        return ".jpg,.jpeg,.png,.gif,.webp,image/jpeg,image/png,image/gif,image/webp";
    }

    public static boolean hasSelectedFiles(MultipartFile[] imageFiles) {
        return !collectSelectedFiles(imageFiles).isEmpty();
    }

    public static List<MultipartFile> collectSelectedFiles(MultipartFile[] imageFiles) {
        if (imageFiles == null || imageFiles.length == 0) {
            return List.of();
        }
        List<MultipartFile> selected = new ArrayList<>();
        for (MultipartFile imageFile : imageFiles) {
            if (imageFile != null && !imageFile.isEmpty()) {
                selected.add(imageFile);
            }
        }
        return selected;
    }

    public static void validateSelectedFiles(MultipartFile[] imageFiles) {
        List<MultipartFile> selectedFiles = collectSelectedFiles(imageFiles);
        if (selectedFiles.isEmpty()) {
            return;
        }
        if (selectedFiles.size() > MAX_IMAGE_COUNT) {
            throw new RuntimeException("单帖最多上传 " + MAX_IMAGE_COUNT + " 张图片");
        }
        for (MultipartFile imageFile : selectedFiles) {
            validateFile(imageFile);
        }
    }

    public static String requireAllowedExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new RuntimeException("图片文件名不能为空");
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0) {
            throw new RuntimeException("图片格式不受支持");
        }
        String extension = originalFilename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("图片仅支持 " + String.join(" / ", ALLOWED_EXTENSIONS) + " 格式");
        }
        return extension;
    }

    private static void validateFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }
        if (imageFile.getSize() > MAX_SINGLE_FILE_SIZE) {
            throw new RuntimeException("单张图片不能超过 " + MAX_SINGLE_FILE_SIZE_MB + "MB");
        }
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("仅支持上传图片文件");
        }
        requireAllowedExtension(imageFile.getOriginalFilename());
    }
}
