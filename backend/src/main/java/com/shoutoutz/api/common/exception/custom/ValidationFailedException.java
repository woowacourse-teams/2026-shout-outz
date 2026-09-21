package com.shoutoutz.api.common.exception.custom;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.response.ErrorResponse;
import java.util.List;

/**
 * 서비스 계층에서 요청 문맥에 따라 조립한 validation 상세 정보를 반환할 때 사용한다.
 */
public class ValidationFailedException extends BadRequestException {

    private final List<ErrorResponse.ErrorDetail> details;

    public ValidationFailedException(List<ErrorResponse.ErrorDetail> details) {
        super(CommonErrorCode.VALIDATION_FAILED);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ErrorResponse.ErrorDetail> getDetails() {
        return details;
    }
}
