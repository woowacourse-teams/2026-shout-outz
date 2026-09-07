package com.shoutoutz.api.visitor.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.visitor.application.VisitorService;
import com.shoutoutz.api.visitor.presentation.dto.request.VisitorSaveRequest;
import com.shoutoutz.api.visitor.presentation.dto.response.VisitorSaveResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/visitors")
@RequiredArgsConstructor
public class VisitorHttpApi {
    private final VisitorService visitorService;

    @PostMapping
    public ResponseEntity<SuccessResponse<VisitorSaveResponse>> createVisitor(
            @Valid @RequestBody VisitorSaveRequest body) {

        VisitorSaveResponse response = visitorService.createVisitor(body);

        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.success(response));
    }
}
