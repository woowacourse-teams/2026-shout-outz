package com.shoutoutz.api.post.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.post.application.PostService;
import com.shoutoutz.api.post.application.dto.PostFindAllResult;
import com.shoutoutz.api.post.presentation.dto.request.PostFindAllRequest;
import com.shoutoutz.api.post.presentation.dto.request.PostSaveRequest;
import com.shoutoutz.api.post.presentation.dto.request.PostUpdateRequest;
import com.shoutoutz.api.post.presentation.dto.response.PostResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostHttpApi {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<PostResponse>>> findAllPost(
            @Valid @ModelAttribute PostFindAllRequest request
    ) {
        PostFindAllResult result = postService.findAllPost(request);
        List<PostResponse> response = PostResponse.from(result.items());
        SliceMetaResponse meta = new SliceMetaResponse(result.nextCursor(), result.hasNext());

        return ResponseEntity.ok(SuccessResponse.success(response, meta));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<SuccessResponse<PostResponse>> findPost(
            @PathVariable long postId
    ) {
        PostResponse response = postService.findPost(postId);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<PostResponse>> savePost(
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody PostSaveRequest request
    ) {
        PostResponse response = postService.savePost(user.userId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(response));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<SuccessResponse<PostResponse>> updatePost(
            @PathVariable long postId,
            @LoginUser AuthenticatedUser user,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        PostResponse response = postService.updatePost(postId, user.userId(), request);

        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable long postId,
            @LoginUser AuthenticatedUser user
    ) {
        postService.deletePost(postId, user.userId());

        return ResponseEntity.noContent().build();
    }
}
