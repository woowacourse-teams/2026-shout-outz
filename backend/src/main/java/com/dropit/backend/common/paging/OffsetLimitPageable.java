package com.dropit.backend.common.paging;

import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetLimitPageable implements Pageable {

    private final int limit;
    private final long offset;
    private final Sort sort;

    public OffsetLimitPageable(int limit, long offset) {
        this(limit, offset, Sort.unsorted());
    }

    public OffsetLimitPageable(int limit, long offset, Sort sort) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit은 1 이상이어야 합니다.");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset은 0 이상이어야 합니다.");
        }
        this.limit = limit;
        this.offset = offset;
        this.sort = Objects.requireNonNull(sort);
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetLimitPageable(limit, offset + limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetLimitPageable(limit, offset - limit, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitPageable(limit, 0, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber는 0 이상이어야 합니다.");
        }
        return new OffsetLimitPageable(limit, (long) pageNumber * limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset >= limit;
    }
}
