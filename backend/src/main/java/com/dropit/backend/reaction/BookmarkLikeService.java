package com.dropit.backend.reaction;

import java.util.List;
import java.util.UUID;

import com.dropit.backend.app.AppEntity;
import com.dropit.backend.app.AppRepository;
import com.dropit.backend.common.api.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookmarkLikeService {

    private final AppRepository appRepository;
    private final AppBookmarkRepository bookmarkRepository;
    private final AppLikeRepository likeRepository;

    public BookmarkLikeService(
        AppRepository appRepository,
        AppBookmarkRepository bookmarkRepository,
        AppLikeRepository likeRepository
    ) {
        this.appRepository = appRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.likeRepository = likeRepository;
    }

    @Transactional(readOnly = true)
    public AppIdsResponse bookmarks(UUID userId) {
        return new AppIdsResponse(bookmarkRepository.findAppIdsByUserId(userId));
    }

    @Transactional
    public void bookmark(UUID userId, String appId) {
        requireVisibleApp(appId);
        bookmarkRepository.insertIfAbsent(userId, appId);
    }

    @Transactional
    public void unbookmark(UUID userId, String appId) {
        bookmarkRepository.deleteRelation(userId, appId);
    }

    @Transactional(readOnly = true)
    public AppIdsResponse likes(UUID userId) {
        return new AppIdsResponse(likeRepository.findAppIdsByUserId(userId));
    }

    @Transactional
    public LikeResponse toggleLike(UUID userId, String appId) {
        requireVisibleApp(appId);
        int inserted = likeRepository.insertIfAbsent(userId, appId);
        boolean liked;
        if (inserted == 1) {
            appRepository.incrementLikes(appId);
            liked = true;
        } else {
            int deleted = likeRepository.deleteRelation(userId, appId);
            if (deleted == 1) {
                appRepository.decrementLikes(appId);
            }
            liked = false;
        }
        AppEntity app = appRepository.findById(appId).orElseThrow(() -> ApiException.notFound("서비스를 찾을 수 없습니다."));
        return new LikeResponse(liked, app.getLikes());
    }

    private AppEntity requireVisibleApp(String appId) {
        AppEntity app = appRepository.findById(appId).orElseThrow(() -> ApiException.notFound("서비스를 찾을 수 없습니다."));
        if (app.getDeletedAt() != null) {
            throw ApiException.notFound("서비스를 찾을 수 없습니다.");
        }
        return app;
    }
}
