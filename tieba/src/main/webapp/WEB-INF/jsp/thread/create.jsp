<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>发布帖子 - 本地贴吧</title>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            var titleInput = document.getElementById('title');
            var contentInput = document.getElementById('content');
            var boardInput = document.getElementById('boardId');
            var threadTypeInput = document.getElementById('threadType');
            var tagInput = document.getElementById('tagNames');
            var imageInput = document.getElementById('imageFiles');
            var titleCounter = document.getElementById('title-counter');
            var contentCounter = document.getElementById('content-counter');
            var imageSelection = document.getElementById('image-selection');
            var imagePreview = document.getElementById('image-preview');
            var previewTitle = document.getElementById('preview-title');
            var previewMeta = document.getElementById('preview-meta');
            var previewContent = document.getElementById('preview-content');
            var previewTags = document.getElementById('preview-tags');
            var draftKey = 'boardchat.compose.draft';

            function syncCounter(input, counter) {
                if (!input || !counter) {
                    return;
                }
                counter.textContent = String((input.value || '').length);
            }

            function syncImageSelection() {
                if (!imageInput || !imageSelection) {
                    return;
                }
                var files = Array.prototype.filter.call(imageInput.files || [], function (file) {
                    return !!file;
                });
                if (!files.length) {
                    imageSelection.textContent = '当前未选择图片，可以直接发文字帖。';
                    if (imagePreview) {
                        imagePreview.innerHTML = '';
                    }
                    return;
                }
                var names = files.slice(0, 3).map(function (file) { return file.name; }).join(' / ');
                if (files.length > 3) {
                    names += ' 等 ' + files.length + ' 张';
                }
                imageSelection.textContent = '已选择：' + names + '。提交后会直接生成图文帖子。';
                if (imagePreview) {
                    imagePreview.innerHTML = '';
                    files.forEach(function (file) {
                        var reader = new FileReader();
                        reader.onload = function (event) {
                            var item = document.createElement('div');
                            item.className = 'preview-image-item';
                            item.innerHTML = '<img src="' + event.target.result + '" alt="' + file.name.replace(/"/g, '&quot;') + '"><span>' + file.name + '</span>';
                            imagePreview.appendChild(item);
                        };
                        reader.readAsDataURL(file);
                    });
                }
            }

            function syncPreview() {
                if (previewTitle) {
                    previewTitle.textContent = (titleInput && titleInput.value.trim()) || '在这里预览你的帖子标题';
                }
                if (previewMeta) {
                    var boardText = boardInput && boardInput.options[boardInput.selectedIndex]
                        ? boardInput.options[boardInput.selectedIndex].text
                        : '未选择版块';
                    var typeText = threadTypeInput && threadTypeInput.options[threadTypeInput.selectedIndex]
                        ? threadTypeInput.options[threadTypeInput.selectedIndex].text
                        : '讨论';
                    previewMeta.textContent = boardText + ' · ' + typeText;
                }
                if (previewContent) {
                    previewContent.textContent = (contentInput && contentInput.value.trim()) || '正文预览会在这里同步显示。';
                }
                if (previewTags) {
                    previewTags.innerHTML = '';
                    var rawTags = tagInput ? tagInput.value.split(/[,，]/) : [];
                    rawTags.map(function (item) { return item.trim(); })
                        .filter(function (item) { return !!item; })
                        .slice(0, 3)
                        .forEach(function (item) {
                            var tag = document.createElement('span');
                            tag.className = 'mini-tag';
                            tag.textContent = '#' + item;
                            previewTags.appendChild(tag);
                        });
                }
            }

            function saveDraft() {
                if (!window.localStorage) {
                    return;
                }
                var draft = {
                    boardId: boardInput ? boardInput.value : '',
                    threadType: threadTypeInput ? threadTypeInput.value : 'DISCUSSION',
                    title: titleInput ? titleInput.value : '',
                    content: contentInput ? contentInput.value : '',
                    tagNames: tagInput ? tagInput.value : ''
                };
                window.localStorage.setItem(draftKey, JSON.stringify(draft));
            }

            function restoreDraft() {
                if (!window.localStorage) {
                    return;
                }
                if ((titleInput && titleInput.value) || (contentInput && contentInput.value) || (tagInput && tagInput.value)) {
                    return;
                }
                try {
                    var raw = window.localStorage.getItem(draftKey);
                    if (!raw) {
                        return;
                    }
                    var draft = JSON.parse(raw);
                    if (boardInput && draft.boardId) {
                        boardInput.value = draft.boardId;
                    }
                    if (threadTypeInput && draft.threadType) {
                        threadTypeInput.value = draft.threadType;
                    }
                    if (titleInput && draft.title) {
                        titleInput.value = draft.title;
                    }
                    if (contentInput && draft.content) {
                        contentInput.value = draft.content;
                    }
                    if (tagInput && draft.tagNames) {
                        tagInput.value = draft.tagNames;
                    }
                } catch (error) {
                    window.localStorage.removeItem(draftKey);
                }
            }

            restoreDraft();
            syncCounter(titleInput, titleCounter);
            syncCounter(contentInput, contentCounter);
            syncImageSelection();
            syncPreview();

            if (titleInput) {
                titleInput.addEventListener('input', function () { syncCounter(titleInput, titleCounter); syncPreview(); saveDraft(); });
            }
            if (contentInput) {
                contentInput.addEventListener('input', function () { syncCounter(contentInput, contentCounter); syncPreview(); saveDraft(); });
            }
            if (imageInput) {
                imageInput.addEventListener('change', syncImageSelection);
            }
            if (boardInput) {
                boardInput.addEventListener('change', function () { syncPreview(); saveDraft(); });
            }
            if (threadTypeInput) {
                threadTypeInput.addEventListener('change', function () { syncPreview(); saveDraft(); });
            }
            if (tagInput) {
                tagInput.addEventListener('input', function () { syncPreview(); saveDraft(); });
            }
            var publishForm = document.querySelector('.publish-form');
            if (publishForm) {
                publishForm.addEventListener('submit', function () {
                    if (window.localStorage) {
                        window.localStorage.removeItem(draftKey);
                    }
                });
            }
        });
    </script>
</head>
<body class="page-compose">
    <jsp:include page="../common/header.jsp" />

    <main class="container compose-shell">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}${backPath}"><c:out value="${backLabel}" /></a>
        </div>

        <section class="panel publish-hero" data-reveal>
            <div>
                <p class="auth-kicker">CREATE THREAD</p>
                <h1 class="composer-title publish-title">图文帖子正式上线</h1>
                <p class="auth-subtitle">统一处理版块选择、帖子类型、标签、图片上传、草稿暂存和发布回流。先整理内容，再决定何时发出去。</p>
            </div>
            <div class="publish-meta-strip">
                <span class="pill">来源 <c:out value="${entrySource}" /></span>
                <span class="pill">版块 <c:out value="${fn:length(boardOptions)}" /> 个可选</span>
                <span class="pill">图片 <c:out value="${imageMaxCount}" /> 张以内</span>
            </div>
        </section>

        <c:if test="${not empty error}">
            <div class="alert alert-error" data-reveal><c:out value="${error}" /></div>
        </c:if>

        <c:choose>
            <c:when test="${boardUnavailable}">
                <section class="panel publish-empty" data-reveal>
                    <h2>当前暂无可发帖版块</h2>
                    <p>页面已可打开，但数据库还没有可用版块。系统会尝试自动补齐默认版块；如果仍为空，请检查初始化脚本是否执行。</p>
                    <div class="publish-actions">
                        <a href="${pageContext.request.contextPath}${backPath}" class="btn btn-ghost">返回上一个页面</a>
                        <a href="${pageContext.request.contextPath}/" class="btn">回到首页</a>
                    </div>
                </section>
            </c:when>
            <c:otherwise>
                <form action="${pageContext.request.contextPath}/board/post/thread"
                      method="post"
                      enctype="multipart/form-data"
                      class="publish-form"
                      data-reveal>
                    <input type="hidden" name="entrySource" value="${entrySource}">

                    <div class="publish-grid">
                        <div class="publish-column">
                            <section class="panel publish-card">
                                <div class="section-head">
                                    <h2>基础信息</h2>
                                    <span class="pill">必填</span>
                                </div>
                                <div class="form-group">
                                    <label class="form-label" for="boardId">发布到哪个版块</label>
                                    <select id="boardId" name="boardId" class="form-control" required>
                                        <c:forEach items="${boardOptions}" var="board">
                                            <option value="${board.id}" <c:if test="${board.id == selectedBoardId}">selected</c:if>>
                                                <c:out value="${board.name}" />
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label class="form-label" for="threadType">帖子类型</label>
                                    <select id="threadType" name="threadType" class="form-control" required>
                                        <c:forEach items="${threadTypeOptions}" var="typeOption">
                                            <option value="${typeOption.code}" <c:if test="${threadType eq typeOption.code}">selected</c:if>>
                                                <c:out value="${typeOption.label}" /> · <c:out value="${typeOption.description}" />
                                            </option>
                                        </c:forEach>
                                    </select>
                                </div>
                                <div class="form-group">
                                    <div class="publish-field-head">
                                        <label class="form-label" for="title">标题</label>
                                        <span class="thread-meta"><strong id="title-counter"><c:out value="${fn:length(title)}" /></strong> / 100</span>
                                    </div>
                                    <input id="title" type="text" name="title" class="form-control" maxlength="100"
                                           value="<c:out value='${title}' />" placeholder="一句话概括你的主题，建议直接、明确" required>
                                </div>
                                <div class="form-group">
                                    <label class="form-label" for="tagNames">标签（最多 3 个，英文逗号分隔）</label>
                                    <input id="tagNames" type="text" name="tagNames" class="form-control" maxlength="40"
                                           value="<c:out value='${tagNames}' />" placeholder="示例：社团, 招新, 春招">
                                </div>
                            </section>

                            <section class="panel publish-card">
                                <div class="section-head">
                                    <h2>内容编辑</h2>
                                    <span class="pill">正文</span>
                                </div>
                                <div class="publish-field-head">
                                    <label class="form-label" for="content">正文内容</label>
                                    <span class="thread-meta"><strong id="content-counter"><c:out value="${fn:length(content)}" /></strong> / 10000</span>
                                </div>
                                <textarea id="content" name="content" class="form-control publish-editor" rows="14" maxlength="10000"
                                          placeholder="把背景、问题、观点和期望回应写完整，后续更容易得到有效讨论。"
                                          required><c:out value="${content}" /></textarea>
                            </section>
                        </div>

                        <div class="publish-column">
                            <section class="panel publish-card">
                                <div class="section-head">
                                    <h2>图文内容</h2>
                                    <span class="pill">已开放</span>
                                </div>
                                <div class="publish-image-drop">
                                    <label class="form-label" for="imageFiles">上传图片</label>
                                    <input id="imageFiles" type="file" name="imageFiles" class="form-control"
                                           accept="${imageAccept}" multiple>
                                    <p class="section-caption">图片会按上传顺序展示在帖子详情页，并自动抽取第一张作为内容卡片封面。</p>
                                </div>
                                <div id="image-selection" class="publish-image-selection">当前未选择图片，可以直接发文字帖。</div>
                                <div id="image-preview" class="preview-image-grid"></div>
                                <div class="publish-rule-grid">
                                    <div class="publish-rule">
                                        <strong>${imageMaxCount} 张</strong>
                                        <span>单帖上限</span>
                                    </div>
                                    <div class="publish-rule">
                                        <strong>${imageMaxFileSizeMb}MB</strong>
                                        <span>单张大小</span>
                                    </div>
                                    <div class="publish-rule">
                                        <strong><c:out value="${imageExtensions}" /></strong>
                                        <span>允许格式</span>
                                    </div>
                                </div>
                                <div class="alert alert-info publish-note">
                                    图片将归档到 `/uploads/${imageUploadSubDirectory}/`，帖子卡片会优先展示首图。
                                </div>
                            </section>

                            <section class="panel publish-card">
                                <div class="section-head">
                                    <h2>实时预览</h2>
                                    <span class="pill">Draft</span>
                                </div>
                                <div class="preview-card">
                                    <div id="preview-meta" class="thread-meta">未选择版块 · 讨论</div>
                                    <h3 id="preview-title">在这里预览你的帖子标题</h3>
                                    <div id="preview-tags" class="thread-tag-row"></div>
                                    <p id="preview-content" class="thread-summary">正文预览会在这里同步显示。</p>
                                </div>
                                <div class="section-caption">草稿会自动暂存在当前浏览器，本地预览不会影响正式发布。</div>
                            </section>

                            <section class="panel publish-card">
                                <div class="section-head">
                                    <h2>发布动作</h2>
                                    <span class="pill">统一回流</span>
                                </div>
                                <p class="section-caption">
                                    <c:choose>
                                        <c:when test="${not empty sessionScope.user}">
                                            发布成功后会直接进入帖子详情页。
                                        </c:when>
                                        <c:otherwise>
                                            当前为访客模式。你可以先整理内容，真正提交时会跳转登录，并自动返回这个发帖页。
                                        </c:otherwise>
                                    </c:choose>
                                </p>
                                <div class="publish-actions">
                                    <button type="submit" class="btn btn-accent">
                                        <c:choose>
                                            <c:when test="${not empty sessionScope.user}">发布帖子</c:when>
                                            <c:otherwise>登录后发布</c:otherwise>
                                        </c:choose>
                                    </button>
                                    <a href="${pageContext.request.contextPath}${backPath}" class="btn btn-ghost">暂不发布</a>
                                </div>
                            </section>
                        </div>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </main>
</body>
</html>
