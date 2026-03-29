package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.entity.ForumCategory;
import com.chatroom.tieba.entity.ForumThreadImage;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.support.ThreadImagePolicy;
import com.chatroom.tieba.support.ThreadInputValidator;
import com.chatroom.tieba.support.ThreadTypeCatalog;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private ForumService forumService;

    @Value("${upload.path:F:/zhao/chatroom/data/uploads}")
    private String uploadRootPath;

    @GetMapping("/{boardId}")
    public String goBoard(@PathVariable("boardId") Integer boardId,
                          @RequestParam(value = "page", defaultValue = "1") int page,
                          @RequestParam(value = "size", defaultValue = "10") int size,
                          @RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "threadType", required = false) String threadType,
                          @RequestParam(value = "tag", required = false) String tag,
                          Model model) {
        ForumBoard board = forumService.getBoardById(boardId);
        PageResult<ThreadVO> pageResult = forumService.getThreadsByBoardPaged(boardId, keyword, threadType, tag, page, size);
        List<ThreadVO> hotThreads = forumService.getHotThreads(10);
        model.addAttribute("board", board);
        model.addAttribute("threads", pageResult.getList());
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("threadType", threadType == null ? "" : threadType.trim());
        model.addAttribute("tag", tag == null ? "" : tag.trim());
        model.addAttribute("threadTypeOptions", ThreadTypeCatalog.options());
        model.addAttribute("hotThreads", hotThreads);
        return "board/board";
    }

    @GetMapping("/post/thread")
    public String showCreateThreadForm(@RequestParam(value = "boardId", required = false) Integer boardId,
                                       @RequestParam(value = "entrySource", defaultValue = "home") String entrySource,
                                       Model model) {
        Map<ForumCategory, List<ForumBoard>> indexData = forumService.getIndexData();
        List<ForumBoard> boards = collectBoards(indexData);
        String normalizedEntrySource = normalizeEntrySource(entrySource);
        if (boards.isEmpty()) {
            exposeCreateThreadPage(model, boards, boardId, normalizedEntrySource, null);
            model.addAttribute("boardUnavailable", true);
            if (!model.containsAttribute("error")) {
                model.addAttribute("error", "当前暂无可发帖版块");
            }
            return "thread/create";
        }
        Integer selectedBoardId = resolveSelectedBoardId(boardId, boards);
        ForumBoard selectedBoard = findBoardById(selectedBoardId, boards);
        exposeCreateThreadPage(model, boards, selectedBoardId, normalizedEntrySource, selectedBoard);
        model.addAttribute("boardUnavailable", false);
        return "thread/create";
    }

    @PostMapping("/post/thread")
    public String createThread(@RequestParam("boardId") Integer boardId,
                               @RequestParam("title") String title,
                               @RequestParam("content") String content,
                               @RequestParam(value = "threadType", defaultValue = "DISCUSSION") String threadType,
                               @RequestParam(value = "tagNames", required = false) String tagNames,
                               @RequestParam(value = "entrySource", defaultValue = "home") String entrySource,
                               @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String normalizedEntrySource = normalizeEntrySource(entrySource);
        redirectAttributes.addFlashAttribute("title", title == null ? "" : title.trim());
        redirectAttributes.addFlashAttribute("content", content == null ? "" : content.trim());
        redirectAttributes.addFlashAttribute("threadType", threadType == null ? "DISCUSSION" : threadType.trim());
        redirectAttributes.addFlashAttribute("tagNames", tagNames == null ? "" : tagNames.trim());
        UserSessionDTO user = currentUser(session);
        if (user == null) {
            return buildLoginRedirect(boardId, normalizedEntrySource);
        }
        List<ForumThreadImage> storedThreadImages = List.of();
        try {
            int validatedBoardId = ThreadInputValidator.requireBoardId(boardId);
            String validatedTitle = ThreadInputValidator.requireTitle(title);
            String validatedContent = ThreadInputValidator.requireContent(content);
            String validatedThreadType = ThreadInputValidator.requireThreadType(threadType);
            List<String> normalizedTags = ThreadInputValidator.normalizeTags(tagNames);
            ThreadImagePolicy.validateSelectedFiles(imageFiles);
            storedThreadImages = storeThreadImages(user.getId(), imageFiles);
            redirectAttributes.addFlashAttribute("title", validatedTitle);
            redirectAttributes.addFlashAttribute("content", validatedContent);
            redirectAttributes.addFlashAttribute("threadType", validatedThreadType);
            redirectAttributes.addFlashAttribute("tagNames", String.join(", ", normalizedTags));
            Long threadId = forumService.createThread(
                    validatedBoardId,
                    user.getId(),
                    validatedTitle,
                    validatedContent,
                    validatedThreadType,
                    normalizedTags,
                    storedThreadImages
            );
            if ("profile".equals(normalizedEntrySource)) {
                return "redirect:/thread/" + threadId + "?fromProfile=true";
            }
            return "redirect:/thread/" + threadId;
        } catch (RuntimeException | IOException ex) {
            cleanupStoredThreadImages(storedThreadImages);
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:" + buildCreateThreadRedirect(boardId, normalizedEntrySource);
        }
    }

    private void exposeCreateThreadPage(Model model,
                                        List<ForumBoard> boards,
                                        Integer selectedBoardId,
                                        String entrySource,
                                        ForumBoard selectedBoard) {
        model.addAttribute("boardOptions", boards);
        model.addAttribute("selectedBoardId", selectedBoardId);
        model.addAttribute("entrySource", entrySource);
        model.addAttribute("backPath", resolveBackPath(entrySource, selectedBoardId));
        model.addAttribute("backLabel", resolveBackLabel(entrySource, selectedBoard));
        model.addAttribute("imageMaxCount", ThreadImagePolicy.MAX_IMAGE_COUNT);
        model.addAttribute("imageMaxFileSizeMb", ThreadImagePolicy.MAX_SINGLE_FILE_SIZE_MB);
        model.addAttribute("imageAccept", ThreadImagePolicy.acceptAttribute());
        model.addAttribute("imageExtensions", String.join(" / ", ThreadImagePolicy.getAllowedExtensions()));
        model.addAttribute("imageUploadSubDirectory", ThreadImagePolicy.UPLOAD_SUB_DIRECTORY);
        model.addAttribute("threadTypeOptions", ThreadTypeCatalog.options());
        if (!model.containsAttribute("title")) {
            model.addAttribute("title", "");
        }
        if (!model.containsAttribute("content")) {
            model.addAttribute("content", "");
        }
        if (!model.containsAttribute("threadType")) {
            model.addAttribute("threadType", "DISCUSSION");
        }
        if (!model.containsAttribute("tagNames")) {
            model.addAttribute("tagNames", "");
        }
    }

    private Integer resolveSelectedBoardId(Integer preferredBoardId, List<ForumBoard> boards) {
        if (preferredBoardId == null || preferredBoardId <= 0) {
            return boards.get(0).getId();
        }
        for (ForumBoard board : boards) {
            if (preferredBoardId.equals(board.getId())) {
                return preferredBoardId;
            }
        }
        return boards.get(0).getId();
    }

    private List<ForumBoard> collectBoards(Map<ForumCategory, List<ForumBoard>> indexData) {
        List<ForumBoard> boards = new ArrayList<>();
        if (indexData == null || indexData.isEmpty()) {
            return boards;
        }
        for (List<ForumBoard> groupBoards : indexData.values()) {
            if (groupBoards == null || groupBoards.isEmpty()) {
                continue;
            }
            boards.addAll(groupBoards);
        }
        return boards;
    }

    private ForumBoard findBoardById(Integer boardId, List<ForumBoard> boards) {
        if (boardId == null || boards == null || boards.isEmpty()) {
            return null;
        }
        for (ForumBoard board : boards) {
            if (boardId.equals(board.getId())) {
                return board;
            }
        }
        return null;
    }

    private String normalizeEntrySource(String entrySource) {
        if ("board".equalsIgnoreCase(entrySource)) {
            return "board";
        }
        if ("profile".equalsIgnoreCase(entrySource)) {
            return "profile";
        }
        return "home";
    }

    private String resolveBackPath(String entrySource, Integer selectedBoardId) {
        if ("board".equals(entrySource) && selectedBoardId != null && selectedBoardId > 0) {
            return "/board/" + selectedBoardId;
        }
        if ("profile".equals(entrySource)) {
            return "/user/profile";
        }
        return "/";
    }

    private String resolveBackLabel(String entrySource, ForumBoard selectedBoard) {
        if ("board".equals(entrySource)) {
            return selectedBoard == null ? "返回吧内列表" : "返回" + selectedBoard.getName() + "吧";
        }
        if ("profile".equals(entrySource)) {
            return "返回个人中心";
        }
        return "返回首页";
    }

    private String buildCreateThreadRedirect(Integer boardId, String entrySource) {
        StringBuilder redirect = new StringBuilder("/board/post/thread");
        boolean hasQuery = false;
        if (boardId != null && boardId > 0) {
            redirect.append("?boardId=").append(boardId);
            hasQuery = true;
        }
        redirect.append(hasQuery ? "&" : "?")
                .append("entrySource=")
                .append(normalizeEntrySource(entrySource));
        return redirect.toString();
    }

    private String buildLoginRedirect(Integer boardId, String entrySource) {
        String returnTo = buildCreateThreadRedirect(boardId, entrySource);
        return "redirect:/auth/login?returnTo=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
    }

    private UserSessionDTO currentUser(HttpSession session) {
        return session == null ? null : (UserSessionDTO) session.getAttribute("user");
    }

    private List<ForumThreadImage> storeThreadImages(Long userId, MultipartFile[] imageFiles) throws IOException {
        List<MultipartFile> selectedFiles = ThreadImagePolicy.collectSelectedFiles(imageFiles);
        if (selectedFiles.isEmpty()) {
            return List.of();
        }
        List<ForumThreadImage> storedImages = new ArrayList<>();
        try {
            int sortNo = 1;
            for (MultipartFile imageFile : selectedFiles) {
                String extension = ThreadImagePolicy.requireAllowedExtension(imageFile.getOriginalFilename());
                Path uploadDirectory = Paths.get(uploadRootPath, ThreadImagePolicy.UPLOAD_SUB_DIRECTORY, String.valueOf(userId));
                Files.createDirectories(uploadDirectory);
                String fileName = System.currentTimeMillis() + "-" + UUID.randomUUID() + "." + extension;
                Path targetPath = uploadDirectory.resolve(fileName);
                try (InputStream inputStream = imageFile.getInputStream()) {
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                ForumThreadImage threadImage = new ForumThreadImage();
                threadImage.setSortNo(sortNo++);
                threadImage.setFilePath("/uploads/" + ThreadImagePolicy.UPLOAD_SUB_DIRECTORY + "/" + userId + "/" + fileName);
                threadImage.setOriginalName(imageFile.getOriginalFilename());
                threadImage.setContentType(imageFile.getContentType());
                threadImage.setFileSize(imageFile.getSize());
                storedImages.add(threadImage);
            }
            return storedImages;
        } catch (IOException ex) {
            cleanupStoredThreadImages(storedImages);
            throw ex;
        }
    }

    private void cleanupStoredThreadImages(List<ForumThreadImage> storedThreadImages) {
        if (storedThreadImages == null || storedThreadImages.isEmpty()) {
            return;
        }
        for (ForumThreadImage storedThreadImage : storedThreadImages) {
            if (storedThreadImage == null || storedThreadImage.getFilePath() == null || storedThreadImage.getFilePath().isBlank()) {
                continue;
            }
            if (!storedThreadImage.getFilePath().startsWith("/uploads/")) {
                continue;
            }
            String relativePath = storedThreadImage.getFilePath().substring("/uploads/".length()).replace("/", File.separator);
            Path targetPath = Paths.get(uploadRootPath, relativePath);
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {
            }
        }
    }
}
