package com.shoutoutz.api.comment.application.dto;

public enum UserCommentType {
    FEED(1),
    PROJECT(2);

    private final int cursorOrder;

    UserCommentType(int cursorOrder) {
        this.cursorOrder = cursorOrder;
    }

    public int cursorOrder() {
        return cursorOrder;
    }
}
