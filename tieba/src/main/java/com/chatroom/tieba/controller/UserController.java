package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.UserProfile;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.service.UserService;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Controller
public class UserController {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final String PUBLIC_UPLOAD_PREFIX = "/uploads/";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${upload.path:F:/zhao/chatroom/data/uploads}")
    private String uploadRootPath;

    @Autowired
    private UserService userService;

    @Autowired
    private ForumService forumService;

    @GetMapping("/user/profile")
    public String showProfile(@RequestParam(value = "myThreadPage", defaultValue = "1") int myThreadPage,
                              @RequestParam(value = "myReplyPage", defaultValue = "1") int myReplyPage,
                              @RequestParam(value = "highlightThreadId", required = false) Long highlightThreadId,
                              HttpSession session,
                              org.springframework.ui.Model model) {
        UserSessionDTO loginUser = session == null ? null : (UserSessionDTO) session.getAttribute("user");
        model.addAttribute("highlightThreadId", highlightThreadId);
        if (loginUser == null) {
            return "user/profile";
        }
        UserProfile profile = userService.getProfileByUserId(loginUser.getId());
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(loginUser.getId());
            profile.setNickname(loginUser.getNickname());
            profile.setAvatarPath(loginUser.getAvatar());
        }
        PageResult<ThreadVO> myThreads = forumService.getThreadsByUser(loginUser.getId(), myThreadPage, 5);
        PageResult<PostVO> myReplies = forumService.getPostsByUser(loginUser.getId(), myReplyPage, 5);

        model.addAttribute("profile", profile);
        model.addAttribute("account", loginUser);
        model.addAttribute("myThreads", myThreads);
        model.addAttribute("myReplies", myReplies);
        return "user/profile";
    }

    @PostMapping("/user/profile/update")
    public String updateProfile(@RequestParam("nickname") String nickname,
                                @RequestParam(value = "bio", required = false) String bio,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        UserSessionDTO loginUser = requireLogin(session);
        UserProfile currentProfile = userService.getProfileByUserId(loginUser.getId());

        String newAvatarPath = null;
        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                newAvatarPath = storeAvatar(loginUser.getId(), avatarFile);
            }

            UserProfile updatedProfile = userService.updateProfile(loginUser.getId(), nickname, bio, newAvatarPath);
            loginUser.setNickname(updatedProfile.getNickname());
            loginUser.setAvatar(updatedProfile.getAvatarPath());
            session.setAttribute("user", loginUser);

            if (newAvatarPath != null && currentProfile != null) {
                deleteOldAvatar(currentProfile.getAvatarPath(), newAvatarPath);
            }

            redirectAttributes.addFlashAttribute("success", "个人资料已保存");
        } catch (RuntimeException | IOException e) {
            cleanupUploadedFile(newAvatarPath);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/user/profile";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "上传失败：头像文件超过服务端限制");
        return "redirect:/user/profile";
    }

    private UserSessionDTO requireLogin(HttpSession session) {
        UserSessionDTO loginUser = (UserSessionDTO) session.getAttribute("user");
        if (loginUser == null) {
            throw new RuntimeException("请先登录后再访问个人中心");
        }
        return loginUser;
    }

    private String storeAvatar(Long userId, MultipartFile avatarFile) throws IOException {
        validateAvatar(avatarFile);

        String extension = resolveExtension(avatarFile.getOriginalFilename());
        Path userAvatarDir = Paths.get(uploadRootPath, "avatars", String.valueOf(userId));
        Files.createDirectories(userAvatarDir);

        String fileName = FILE_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + UUID.randomUUID() + extension;
        Path targetPath = userAvatarDir.resolve(fileName);
        try (InputStream inputStream = avatarFile.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return PUBLIC_UPLOAD_PREFIX + "avatars/" + userId + "/" + fileName;
    }

    private void validateAvatar(MultipartFile avatarFile) {
        if (avatarFile.getSize() > MAX_AVATAR_SIZE) {
            throw new RuntimeException("头像文件不能超过 2MB");
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("仅支持上传图片格式的头像");
        }

        String extension = resolveExtension(avatarFile.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("头像仅支持 jpg、png、gif、webp 格式");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new RuntimeException("无法识别上传文件名");
        }

        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0) {
            throw new RuntimeException("头像文件缺少扩展名");
        }

        return originalFilename.substring(lastDot).toLowerCase(Locale.ROOT);
    }

    private void deleteOldAvatar(String oldAvatarPath, String newAvatarPath) {
        if (oldAvatarPath == null || oldAvatarPath.isBlank() || oldAvatarPath.equals(newAvatarPath)) {
            return;
        }
        if (!oldAvatarPath.startsWith(PUBLIC_UPLOAD_PREFIX)) {
            return;
        }

        String relativePath = oldAvatarPath.substring(PUBLIC_UPLOAD_PREFIX.length()).replace("/", File.separator);
        Path oldFilePath = Paths.get(uploadRootPath, relativePath);
        try {
            Files.deleteIfExists(oldFilePath);
        } catch (IOException ignored) {
        }
    }

    private void cleanupUploadedFile(String avatarPath) {
        if (avatarPath == null || !avatarPath.startsWith(PUBLIC_UPLOAD_PREFIX)) {
            return;
        }

        String relativePath = avatarPath.substring(PUBLIC_UPLOAD_PREFIX.length()).replace("/", File.separator);
        Path filePath = Paths.get(uploadRootPath, relativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }
}
