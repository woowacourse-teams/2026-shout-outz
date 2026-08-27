package com.shoutoutz.api.visitor.application;

import com.shoutoutz.api.visitor.domain.Visitor;
import com.shoutoutz.api.visitor.domain.VisitorRepository;
import com.shoutoutz.api.visitor.presentation.dto.request.VisitorSaveRequest;
import com.shoutoutz.api.visitor.presentation.dto.response.VisitorSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VisitorService {

    private final VisitorRepository visitorRepository;

    public VisitorSaveResponse createVisitor(VisitorSaveRequest body) {
        Visitor visitor = Visitor.initialize(body.example());
        Visitor savedVisitor = visitorRepository.save(visitor);
        return new VisitorSaveResponse(savedVisitor.getId(),  savedVisitor.getExample());
    }
}
