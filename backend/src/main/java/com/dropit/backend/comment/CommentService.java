package com.dropit.backend.comment;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dropit.backend.app.AppRepository;
import com.dropit.backend.common.api.ApiException;
import com.dropit.backend.maker.MakerEntity;
import com.dropit.backend.maker.MakerRepository;
import com.dropit.backend.maker.MakerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final AppRepository appRepository;
    private final AppCommentRepository commentRepository;
    private final MakerRepository makerRepository;

    public CommentService(
        AppRepository appRepository,
        AppCommentRepository commentRepository,
        MakerRepository makerRepository
    ) {
        this.appRepository = appRepository;
        this.commentRepository = commentRepository;
        this.makerRepository = makerRepository;
    }

    @Transactional(readOnly = true)
    public CommentListResponse list(String appId) {
        requireVisibleApp(appId);
        List<AppCommentEntity> comments = commentRepository.findAllByAppIdOrderByCreatedAtDesc(appId);
        Map<UUID, MakerEntity> authors = makerRepository.findAllById(
            comments.stream().map(AppCommentEntity::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(MakerEntity::getId, Function.identity()));

        return new CommentListResponse(comments.stream()
            .map(comment -> toResponse(comment, authors.get(comment.getUserId())))
            .toList());
    }

    @Transactional
    public CommentResponse create(String appId, UUID userId, CommentRequest request) {
        requireVisibleApp(appId);
        MakerEntity author = makerRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("댓글을 작성하려면 프로필을 먼저 등록해주세요."));

        UUID parentId = request.parentId();
        if (parentId != null) {
            AppCommentEntity parent = commentRepository.findById(parentId)
                .orElseThrow(() -> ApiException.notFound("답글을 남길 댓글을 찾을 수 없습니다."));
            if (!parent.getAppId().equals(appId)) {
                throw ApiException.notFound("답글을 남길 댓글을 찾을 수 없습니다.");
            }
            if (parent.getParentId() != null) {
                throw ApiException.badRequest("답글에는 다시 답글을 달 수 없습니다.");
            }
            if (request.title() != null && !request.title().trim().isEmpty()) {
                throw ApiException.badRequest("답글에는 제목을 남길 수 없습니다.");
            }
        }

        String title = parentId == null ? normalizeTitle(request.title()) : "";
        AppCommentEntity comment = new AppCommentEntity(appId, userId, parentId, title, request.content().trim());
        return toResponse(commentRepository.save(comment), author);
    }

    @Transactional
    public void delete(UUID userId, UUID commentId) {
        AppCommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> ApiException.notFound("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw ApiException.forbidden("댓글 작성자만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }

    private void requireVisibleApp(String appId) {
        appRepository.findById(appId)
            .filter(app -> app.getDeletedAt() == null)
            .orElseThrow(() -> ApiException.notFound("서비스를 찾을 수 없습니다."));
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private CommentResponse toResponse(AppCommentEntity comment, MakerEntity author) {
        if (author == null) {
            throw ApiException.notFound("댓글 작성자의 프로필을 찾을 수 없습니다.");
        }
        return new CommentResponse(
            comment.getId(),
            comment.getAppId(),
            comment.getUserId(),
            comment.getParentId(),
            comment.getTitle(),
            comment.getContent(),
            comment.getCreatedAt(),
            MakerResponse.from(author)
        );
    }
}
