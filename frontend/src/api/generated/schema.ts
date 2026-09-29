/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

/** AdminProjectApproveSuccessResponse */
export interface AdminProjectApproveSuccessResponse {
  /** 승인 결과 */
  data: {
    /** 변경된 승인 상태 */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 승인 시각 */
    decidedAt: string;
    /** 승인한 관리자 */
    decidedBy: {
      /** 관리자 handle */
      handle: string;
      /** 관리자 사용자 ID */
      userId: number;
    };
    /** 프로젝트 ID */
    projectId: number;
  };
  /** 응답 상태 */
  status: string;
}

/** AdminProjectDetailSuccessResponse */
export interface AdminProjectDetailSuccessResponse {
  /** 프로젝트 상세 */
  data: {
    /** 승인 상태 */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부 */
    bookmarkedByMe: boolean;
    /** 우아한테크코스 기수 */
    cohort: number;
    /** 댓글 수 */
    commentCount: number;
    /** 등록 시각 */
    createdAt: string;
    /** 배포 URL */
    deploymentUrl?: string | null;
    /** 프로젝트 설명 */
    descriptionMd?: string | null;
    /** 본문 이미지 */
    descriptionMedia: (object | boolean | string | number)[];
    /** GitHub 저장소 URL */
    githubRepositoryUrl: string;
    /** 프로젝트 ID */
    id: number;
    /** 프로젝트 썸네일 URL */
    imageUrl?: string | null;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부 */
    likedByMe: boolean;
    /** 프로젝트 팀원 */
    members: (object | boolean | string | number)[];
    /** 프로젝트 등록자 ID */
    registeredBy?: number | null;
    /** 반려 사유 */
    rejectReason?: string | null;
    /** 운영 상태 */
    serviceStatus: "OPERATING" | "CLOSED";
    /** 프로젝트 slug */
    slug: string;
    /** GitHub star 수 */
    starCount?: number | null;
    /** 한 줄 소개 */
    tagline: string;
    /** 팀 이름 */
    teamName: string;
    /** 기술 스택 */
    techTags: (object | boolean | string | number)[];
    /** 프로젝트 썸네일 이미지 ID */
    thumbnailImageId?: number | null;
    /** 프로젝트 이름 */
    title: string;
    /** 수정 시각 */
    updatedAt: string;
    /** 조회 수 */
    viewCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** AdminProjectFindAllSuccessResponse */
export interface AdminProjectFindAllSuccessResponse {
  /** 프로젝트 심사 목록 */
  data: {
    /** 승인 상태 */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부 */
    bookmarkedByMe: boolean;
    /** 우아한테크코스 기수 */
    cohort: number;
    /** 댓글 수 */
    commentCount: number;
    /** 등록 시각 */
    createdAt: string;
    /** 프로젝트 ID */
    id: number;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부 */
    likedByMe: boolean;
    /** 프로젝트 팀원 */
    members: (object | boolean | string | number)[];
    /** 프로젝트 등록자 ID */
    registeredBy?: number | null;
    /** 반려 사유. REJECTED일 때만 값이 있다. */
    rejectReason?: string | null;
    /** 운영 상태 */
    serviceStatus: "OPERATING" | "CLOSED";
    /** 프로젝트 slug */
    slug: string;
    /** GitHub star 수 */
    starCount?: number | null;
    /** 한 줄 소개 */
    tagline: string;
    /** 팀 이름 */
    teamName: string;
    /** 기술 스택 */
    techTags: (object | boolean | string | number)[];
    /** 프로젝트 썸네일 이미지 ID */
    thumbnailImageId?: number | null;
    /** 프로젝트 썸네일 URL */
    thumbnailUrl?: string | null;
    /** 프로젝트 이름 */
    title: string;
    /** 수정 시각 */
    updatedAt: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 커서 */
    nextCursor?: string | null;
    /** 전체 프로젝트 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** AdminProjectHistorySuccessResponse */
export interface AdminProjectHistorySuccessResponse {
  /** 프로젝트 심사 이력 */
  data: {
    /** 상태 변경 이력 */
    items: {
      /** 상태 변경 시각 */
      changedAt: string;
      /** 상태를 변경한 사용자. 최초 등록이면 null */
      changedBy?: object | null;
      /** 변경 전 상태. 최초 등록이면 null */
      fromStatus?: string | null;
      /** 이력 ID */
      historyId: number;
      /** 반려 사유 */
      reason?: string | null;
      /** 변경 후 상태 */
      toStatus: "PENDING" | "APPROVED" | "REJECTED";
    }[];
    /** 프로젝트 ID */
    projectId: number;
  };
  /** 응답 상태 */
  status: string;
}

/** AdminProjectRejectRequest */
export interface AdminProjectRejectRequest {
  /** 반려 사유(앞뒤 공백 제거 후 1~100자) */
  reason: string;
}

/** AdminProjectRejectSuccessResponse */
export interface AdminProjectRejectSuccessResponse {
  /** 반려 결과 */
  data: {
    /** 변경된 승인 상태 */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 반려 시각 */
    decidedAt: string;
    /** 반려한 관리자 */
    decidedBy: {
      /** 관리자 handle */
      handle: string;
      /** 관리자 사용자 ID */
      userId: number;
    };
    /** 프로젝트 ID */
    projectId: number;
    /** 반려 사유 */
    reason: string;
  };
  /** 응답 상태 */
  status: string;
}

/** AdminVerificationRequestApproveSuccessResponse */
export interface AdminVerificationRequestApproveSuccessResponse {
  /** 승인 결과 */
  data: {
    /** 승인 시각(ISO-8601) */
    decidedAt: string;
    /** 승인한 관리자 */
    decidedBy: {
      /** 승인한 관리자 handle */
      handle: string;
      /** 승인한 관리자 사용자 ID */
      userId: number;
    };
    /** 인증 신청 ID */
    requestId: number;
    /** 변경된 신청 상태 */
    status: "PENDING" | "APPROVED" | "REJECTED";
  };
  /** 응답 상태 */
  status: string;
}

/** AdminVerificationRequestFindAllSuccessResponse */
export interface AdminVerificationRequestFindAllSuccessResponse {
  /** 인증 신청 목록 */
  data: {
    /** 신청자 식별 정보 */
    applicant: {
      /** 신청자 handle */
      handle: string;
      /** 신청자 사용자 ID */
      userId: number;
    };
    /** 신청 기수. 코치 신청은 null */
    cohort?: number | null;
    /** 신청 닉네임 */
    nickname: string;
    /** 인증 신청 ID */
    requestId: number;
    /** 신청 시각(ISO-8601) */
    requestedAt: string;
    /** 현재 신청 상태 */
    status: "PENDING" | "APPROVED" | "REJECTED";
    /** 신청 트랙. 코치 신청은 null */
    track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
    /** 신청 유형 */
    userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 커서. 다음 페이지가 없으면 null */
    nextCursor?: string | null;
    /** 커서와 size를 제외한 상태 조건을 만족하는 전체 인증 신청 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** AdminVerificationRequestHistorySuccessResponse */
export interface AdminVerificationRequestHistorySuccessResponse {
  /** 인증 신청 이력 */
  data: {
    /** 상태 변경 이력(최신순) */
    items: {
      /** 상태 변경 시각(ISO-8601) */
      changedAt: string;
      /** 상태를 변경한 관리자. 최초 신청이면 null */
      changedBy?: {
        /** 상태를 변경한 관리자 handle */
        handle?: string | null;
        /** 상태를 변경한 관리자 사용자 ID */
        userId?: number | null;
      };
      /** 변경 전 상태. 최초 신청이면 null */
      fromStatus?: "PENDING" | "APPROVED" | "REJECTED" | null;
      /** 이력 ID */
      historyId: number;
      /** 반려 사유. 승인 또는 최초 신청이면 null */
      reason?: string | null;
      /** 변경 후 상태 */
      toStatus: "PENDING" | "APPROVED" | "REJECTED";
    }[];
    /** 인증 신청 ID */
    requestId: number;
  };
  /** 응답 상태 */
  status: string;
}

/** AdminVerificationRequestRejectRequest */
export interface AdminVerificationRequestRejectRequest {
  /** 반려 사유(앞뒤 공백 제거 후 1~100자) */
  reason: string;
}

/** AdminVerificationRequestRejectSuccessResponse */
export interface AdminVerificationRequestRejectSuccessResponse {
  /** 반려 결과 */
  data: {
    /** 반려 시각(ISO-8601) */
    decidedAt: string;
    /** 반려한 관리자 */
    decidedBy: {
      /** 반려한 관리자 handle */
      handle: string;
      /** 반려한 관리자 사용자 ID */
      userId: number;
    };
    /** 반려 사유 */
    reason: string;
    /** 인증 신청 ID */
    requestId: number;
    /** 변경된 신청 상태 */
    status: "PENDING" | "APPROVED" | "REJECTED";
  };
  /** 응답 상태 */
  status: string;
}

/** AuthSessionSuccessResponse */
export interface AuthSessionSuccessResponse {
  /** 인증 세션 정보 */
  data: {
    /** 상태 변경 요청에 사용할 CSRF Token */
    csrfToken: string;
    /** 인증된 사용자 권한 */
    role?: "USER" | "ADMIN" | null;
    /** 세션 인증 상태 */
    status: "UNAUTHENTICATED" | "SIGNUP_REQUIRED" | "AUTHENTICATED";
    /** 인증된 사용자 ID */
    userId?: number | null;
  };
  /** 응답 상태 */
  status: string;
}

/** CategoryFindAllSuccessResponse */
export interface CategoryFindAllSuccessResponse {
  /** 활성 카테고리 목록 */
  data: {
    /** 카테고리 ID */
    categoryId: number;
    /** 표시 이름 */
    displayName: string;
    /** 표시 순서 */
    displayOrder: number;
    /** 카테고리 slug */
    slug: string;
    /** 카테고리 유형 */
    type: "GENERAL" | "EVENT";
  }[];
  /** 응답 상태 */
  status: string;
}

/** CategorySaveRequest */
export interface CategorySaveRequest {
  /** 고유 표시 이름 */
  displayName: string;
  /** 0 이상 32767 이하 표시 순서 */
  displayOrder: number;
  /** 영문 소문자, 숫자, 하이픈으로 구성된 고유 slug */
  slug: string;
  /** 카테고리 유형 */
  type: "GENERAL" | "EVENT";
}

/** CategorySaveSuccessResponse */
export interface CategorySaveSuccessResponse {
  /** 카테고리 */
  data: {
    /** 활성 여부 */
    active: boolean;
    /** 카테고리 ID */
    categoryId: number;
    /** 표시 이름 */
    displayName: string;
    /** 표시 순서 */
    displayOrder: number;
    /** 카테고리 slug */
    slug: string;
    /** 카테고리 유형 */
    type: "GENERAL" | "EVENT";
  };
  /** 응답 상태 */
  status: string;
}

/** CategoryUpdateRequest */
export interface CategoryUpdateRequest {
  /** 변경할 고유 표시 이름 */
  displayName: string;
  /** 변경할 표시 순서 */
  displayOrder: number;
}

/** CategoryUpdateSuccessResponse */
export interface CategoryUpdateSuccessResponse {
  /** 카테고리 */
  data: {
    /** 활성 여부 */
    active: boolean;
    /** 카테고리 ID */
    categoryId: number;
    /** 표시 이름 */
    displayName: string;
    /** 표시 순서 */
    displayOrder: number;
    /** 카테고리 slug */
    slug: string;
    /** 카테고리 유형 */
    type: "GENERAL" | "EVENT";
  };
  /** 응답 상태 */
  status: string;
}

/** CohortFindAllSuccessResponse */
export interface CohortFindAllSuccessResponse {
  /** 기수 목록 */
  data: {
    /** 기수 목록 (최신 기수부터) */
    items: {
      /** 기수 */
      cohort: number;
      /** 진행 연도 */
      year: number;
    }[];
  };
  /** 응답 상태 */
  status: string;
}

/** ErrorResponse */
export interface ErrorResponse {
  /** 오류 코드 */
  code: string;
  /** 필드별 오류 상세 */
  details?: {
    /** 오류 필드명 */
    field?: string | null;
    /** 필드 오류 메시지 */
    message?: string | null;
  }[];
  /** 오류 메시지 */
  message: string;
  /** 응답 상태 */
  status: string;
}

/** EventCreateRequest */
export interface EventCreateRequest {
  /** 이벤트 작성자 이름 */
  authorName: string;
  /** 이벤트 본문 */
  body: string;
  /** 이벤트 CTA */
  cta?: {
    /** CTA 라벨 */
    label: string;
    /** CTA URL */
    url: string;
  };
  /** 이벤트 종료 시각 */
  eventEndAt: string;
  /** 이벤트 시작 시각 */
  eventStartAt: string;
  /** 이벤트 요약 */
  summary: string;
  /** 이벤트 제목 */
  title: string;
}

/** EventCreateSuccessResponse */
export interface EventCreateSuccessResponse {
  /** 생성된 이벤트 */
  data: {
    /** 이벤트 작성자 */
    author: {
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 이름 */
      name: string;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 이벤트 본문 */
    body: string;
    /** 이벤트 CTA */
    cta?: {
      /** CTA 라벨 */
      label: string;
      /** CTA URL */
      url: string;
    };
    /** 이벤트 종료 시각 */
    eventEndAt: string;
    /** 이벤트 시작 시각 */
    eventStartAt: string;
    /** 이벤트 상태 */
    eventStatus: "UPCOMING" | "ONGOING" | "ENDED";
    /** 이벤트 ID */
    id: number;
    /** 고정 여부 */
    isPinned: boolean;
    /** 고정 순서 */
    pinOrder?: number | null;
    /** 게시 시각 */
    publishedAt: string;
    /** 이벤트 요약 */
    summary: string;
    /** 이벤트 제목 */
    title: string;
    /** 소식 유형. 이벤트 등록이므로 항상 EVENT다. */
    type: "NOTICE" | "EVENT";
  };
  /** 응답 상태 */
  status: string;
}

/** FeedCommentCreateRequest */
export interface FeedCommentCreateRequest {
  /** 앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준) */
  content: string;
  /** 같은 피드의 삭제되지 않은 루트 댓글 ID. 없으면 루트 댓글 */
  parentId?: number | null;
}

/** FeedCommentCreateSuccessResponse */
export interface FeedCommentCreateSuccessResponse {
  /** 생성된 댓글 */
  data: {
    /** 댓글 작성자 */
    author: {
      /** 작성자 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 저장된 댓글 내용 */
    content: string;
    /** 생성 시각 (UTC ISO-8601) */
    createdAt: string;
    /** 현재 사용자가 수정할 수 있는지 여부 */
    editable: boolean;
    /** 댓글 ID */
    id: number;
    /** 부모 댓글 ID */
    parentId?: number | null;
    /** 수정 시각 (UTC ISO-8601) */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedCommentDeleteSuccessResponse */
export interface FeedCommentDeleteSuccessResponse {
  /** 삭제된 댓글 */
  data: {
    /** 댓글 삭제 여부 */
    deleted: boolean;
    /** 삭제된 댓글 ID */
    id: number;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedCommentFindAllSuccessResponse */
export interface FeedCommentFindAllSuccessResponse {
  /** 피드 댓글 목록 */
  data: {
    /** 댓글 공감 수 */
    agreeCount: number;
    /** 현재 사용자의 공감 여부. 비로그인이면 false */
    agreedByMe: boolean;
    /** 댓글 작성자 */
    author: {
      /** 작성자 프로필 이미지 미디어 ID */
      avatarImageId?: number | null;
      /** 작성자 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 댓글 내용. 삭제된 댓글은 null */
    content?: string | null;
    /** 생성 시각 (UTC ISO-8601) */
    createdAt: string;
    /** 댓글이 삭제되었는지 여부 */
    deleted: boolean;
    /** 현재 사용자가 수정할 수 있는지 여부 */
    editable: boolean;
    /** 댓글 내용이 수정된 적이 있는지 여부 */
    edited: boolean;
    /** 댓글 ID */
    id: number;
    /** 부모 루트 댓글 ID */
    parentId?: number | null;
    /** 수정 시각 (UTC ISO-8601) */
    updatedAt: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 cursor */
    nextCursor?: string | null;
    /** 커서와 size에 무관한 삭제되지 않은 루트 댓글과 답글의 전체 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedCommentReactionSuccessResponse */
export interface FeedCommentReactionSuccessResponse {
  /** 피드 댓글 반응 변경 결과 */
  data: {
    /** 요청한 반응의 활성 상태 */
    active: boolean;
    /** 댓글 공감 수 */
    agreeCount: number;
    /** 댓글 ID */
    commentId: number;
    /** 피드 ID */
    feedId: number;
    /** 반응 타입(현재 AGREE) */
    type: string;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedCommentUpdateRequest */
export interface FeedCommentUpdateRequest {
  /** 앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준) */
  content: string;
}

/** FeedCommentUpdateSuccessResponse */
export interface FeedCommentUpdateSuccessResponse {
  /** 수정된 댓글 */
  data: {
    /** 댓글 작성자 */
    author: {
      /** 작성자 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 저장된 댓글 내용 */
    content: string;
    /** 생성 시각 (UTC ISO-8601) */
    createdAt: string;
    /** 현재 사용자가 수정할 수 있는지 여부 */
    editable: boolean;
    /** 댓글 내용이 수정된 적이 있는지 여부 */
    edited: boolean;
    /** 댓글 ID */
    id: number;
    /** 부모 댓글 ID */
    parentId?: number | null;
    /** 수정 시각 (UTC ISO-8601, 변경 없으면 기존 값 유지) */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedFindAllSuccessResponse */
export interface FeedFindAllSuccessResponse {
  /** 피드 목록 */
  data: {
    /** 현재 작성자 프로필 */
    author: {
      /** 현재 프로필 이미지 미디어 ID */
      avatarImageId?: number | null;
      /** 현재 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 이름 */
      displayName: string;
      /** 작성자 핸들 */
      handle: string;
      /** 작성자 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
    };
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부. 비로그인이면 false */
    bookmarkedByMe: boolean;
    /** 카테고리 목록 */
    categories: {
      /** 카테고리 ID */
      categoryId: number;
      /** 카테고리 표시 이름 */
      displayName: string;
      /** 카테고리 slug */
      slug: string;
      /** 카테고리 유형 */
      type: "GENERAL" | "EVENT";
    }[];
    /** 삭제되지 않은 댓글 수 */
    commentCount: number;
    /** Markdown 본문 */
    content: string;
    /** ISO-8601 생성 시각 */
    createdAt: string;
    /** 피드 ID */
    feedId: number;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부. 비로그인이면 false */
    likedByMe: boolean;
    /** 본문 미디어 목록 */
    media: {
      /** 미디어 표시 순서 */
      displayOrder: number;
      /** 본문 미디어 ID */
      mediaId: number;
      /** 본문 미디어 공개 URL */
      url: string;
    }[];
    /** 피드 제목 */
    title: string;
    /** ISO-8601 수정 시각 */
    updatedAt: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 커서 */
    nextCursor?: string | null;
    /** 커서와 size를 제외한 조회 조건을 만족하는 전체 피드 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedFindSuccessResponse */
export interface FeedFindSuccessResponse {
  /** 피드 */
  data: {
    /** 현재 작성자 프로필 */
    author: {
      /** 현재 프로필 이미지 미디어 ID */
      avatarImageId?: number | null;
      /** 현재 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 이름 */
      displayName: string;
      /** 작성자 핸들 */
      handle: string;
      /** 작성자 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
    };
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부. 비로그인이면 false */
    bookmarkedByMe: boolean;
    /** 카테고리 목록 */
    categories: {
      /** 카테고리 ID */
      categoryId: number;
      /** 카테고리 표시 이름 */
      displayName: string;
      /** 카테고리 slug */
      slug: string;
      /** 카테고리 유형 */
      type: "GENERAL" | "EVENT";
    }[];
    /** 삭제되지 않은 댓글 수 */
    commentCount: number;
    /** Markdown 본문 */
    content: string;
    /** ISO-8601 생성 시각 */
    createdAt: string;
    /** 피드 ID */
    feedId: number;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부. 비로그인이면 false */
    likedByMe: boolean;
    /** 본문 미디어 목록 */
    media: {
      /** 미디어 표시 순서 */
      displayOrder: number;
      /** 본문 미디어 ID */
      mediaId: number;
      /** 본문 미디어 공개 URL */
      url: string;
    }[];
    /** 피드 제목 */
    title: string;
    /** ISO-8601 수정 시각 */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedReactionSuccessResponse */
export interface FeedReactionSuccessResponse {
  /** 피드 반응 변경 결과 */
  data: {
    /** 요청한 반응의 활성 상태 */
    active: boolean;
    /** 피드 북마크 수 */
    bookmarkCount: number;
    /** 피드 ID */
    feedId: number;
    /** 피드 좋아요 수 */
    likeCount: number;
    /** 반응 타입(LIKE 또는 BOOKMARK) */
    type: string;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedSaveRequest */
export interface FeedSaveRequest {
  /** 활성 카테고리 ID 목록(일반 1개, 이벤트 개수 제한 없음, 중복 불가) */
  categoryIds: (object | boolean | string | number)[];
  /** Markdown 본문(공백 제외 1자 이상, Unicode 최대 5,000자) */
  content: string;
  /** 작성자가 업로드한 READY FEED_CONTENT 미디어 ID 목록 */
  mediaIds: (object | boolean | string | number)[];
  /** 피드 제목(공백 제외 1자 이상, Unicode 최대 100자) */
  title: string;
}

/** FeedSaveSuccessResponse */
export interface FeedSaveSuccessResponse {
  /** 피드 */
  data: {
    /** 현재 작성자 프로필 */
    author: {
      /** 현재 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 이름 */
      displayName: string;
      /** 작성자 핸들 */
      handle: string;
      /** 작성자 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
    };
    /** 카테고리 목록 */
    categories: {
      /** 카테고리 ID */
      categoryId: number;
      /** 카테고리 표시 이름 */
      displayName: string;
      /** 카테고리 slug */
      slug: string;
      /** 카테고리 유형 */
      type: "GENERAL" | "EVENT";
    }[];
    /** Markdown 본문 */
    content: string;
    /** ISO-8601 생성 시각 */
    createdAt: string;
    /** 피드 ID */
    feedId: number;
    /** 본문 미디어 목록 */
    media: {
      /** 미디어 표시 순서 */
      displayOrder: number;
      /** 본문 미디어 공개 URL */
      url: string;
    }[];
    /** 피드 제목 */
    title: string;
    /** ISO-8601 수정 시각 */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** FeedTitleSuggestionsSuccessResponse */
export interface FeedTitleSuggestionsSuccessResponse {
  /** 피드 제목 자동완성 후보 문자열 목록 */
  data: (object | boolean | string | number)[];
  /** 응답 상태 */
  status: string;
}

/** FeedUpdateRequest */
export interface FeedUpdateRequest {
  /** 변경할 카테고리 ID 목록(일반 1개, 이벤트 개수 제한 없음) */
  categoryIds: (object | boolean | string | number)[];
  /** 변경할 Markdown 본문(공백 제외 1자 이상, Unicode 최대 5,000자) */
  content: string;
  /** 변경할 본문 미디어 ID 목록 */
  mediaIds: (object | boolean | string | number)[];
  /** 변경할 피드 제목 */
  title: string;
}

/** FeedUpdateSuccessResponse */
export interface FeedUpdateSuccessResponse {
  /** 피드 */
  data: {
    /** 현재 작성자 프로필 */
    author: {
      /** 현재 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 이름 */
      displayName: string;
      /** 작성자 핸들 */
      handle: string;
      /** 작성자 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
    };
    /** 카테고리 목록 */
    categories: {
      /** 카테고리 ID */
      categoryId: number;
      /** 카테고리 표시 이름 */
      displayName: string;
      /** 카테고리 slug */
      slug: string;
      /** 카테고리 유형 */
      type: "GENERAL" | "EVENT";
    }[];
    /** Markdown 본문 */
    content: string;
    /** ISO-8601 생성 시각 */
    createdAt: string;
    /** 피드 ID */
    feedId: number;
    /** 본문 미디어 목록 */
    media: {
      /** 미디어 표시 순서 */
      displayOrder: number;
      /** 본문 미디어 공개 URL */
      url: string;
    }[];
    /** 피드 제목 */
    title: string;
    /** ISO-8601 수정 시각 */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** HomeBannerAdminFindAllSuccessResponse */
export interface HomeBannerAdminFindAllSuccessResponse {
  /** 홈 배너 데이터 */
  data: {
    /** 활성 여부 */
    active: boolean;
    /** 배너 ID */
    bannerId: number;
    /** 생성 시각 */
    createdAt: string;
    /** 등록 관리자 ID */
    createdBy: number;
    /** 이동 방식 */
    destinationType: "TARGET" | "URL";
    /** 표시 순서 */
    displayOrder: number;
    /** 표시용 이미지 URL */
    imageUrl: string;
    /** URL 유형 */
    linkType?: "INTERNAL_PATH" | "EXTERNAL_URL" | null;
    /** 내부 경로 또는 외부 HTTPS URL */
    linkUrl?: string | null;
    /** 미디어 ID */
    mediaId: number;
    /** 대상 리소스 ID. 대상이 뉴스·피드일 때만 값이 있다. */
    targetId?: number | null;
    /** 대상 프로젝트 slug. 대상이 프로젝트일 때만 값이 있다. */
    targetSlug?: string | null;
    /** 대상 리소스 유형 */
    targetType?: "NEWS" | "PROJECT" | "FEED" | null;
    /** 수정 시각 */
    updatedAt: string;
  }[];
  /** 응답 상태 */
  status: string;
}

/** HomeBannerAdminSaveSuccessResponse */
export interface HomeBannerAdminSaveSuccessResponse {
  /** 홈 배너 데이터 */
  data: {
    /** 활성 여부 */
    active: boolean;
    /** 배너 ID */
    bannerId: number;
    /** 생성 시각 */
    createdAt: string;
    /** 등록 관리자 ID */
    createdBy: number;
    /** 이동 방식 */
    destinationType: "TARGET" | "URL";
    /** 표시 순서 */
    displayOrder: number;
    /** 표시용 이미지 URL */
    imageUrl: string;
    /** URL 유형 */
    linkType?: "INTERNAL_PATH" | "EXTERNAL_URL" | null;
    /** 내부 경로 또는 외부 HTTPS URL */
    linkUrl?: string | null;
    /** 미디어 ID */
    mediaId: number;
    /** 대상 리소스 ID. 대상이 뉴스·피드일 때만 값이 있다. */
    targetId?: number | null;
    /** 대상 프로젝트 slug. 대상이 프로젝트일 때만 값이 있다. */
    targetSlug?: string | null;
    /** 대상 리소스 유형 */
    targetType?: "NEWS" | "PROJECT" | "FEED" | null;
    /** 수정 시각 */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** HomeBannerAdminUpdateSuccessResponse */
export interface HomeBannerAdminUpdateSuccessResponse {
  /** 홈 배너 데이터 */
  data: {
    /** 활성 여부 */
    active: boolean;
    /** 배너 ID */
    bannerId: number;
    /** 생성 시각 */
    createdAt: string;
    /** 등록 관리자 ID */
    createdBy: number;
    /** 이동 방식 */
    destinationType: "TARGET" | "URL";
    /** 표시 순서 */
    displayOrder: number;
    /** 표시용 이미지 URL */
    imageUrl: string;
    /** URL 유형 */
    linkType?: "INTERNAL_PATH" | "EXTERNAL_URL" | null;
    /** 내부 경로 또는 외부 HTTPS URL */
    linkUrl?: string | null;
    /** 미디어 ID */
    mediaId: number;
    /** 대상 리소스 ID. 대상이 뉴스·피드일 때만 값이 있다. */
    targetId?: number | null;
    /** 대상 프로젝트 slug. 대상이 프로젝트일 때만 값이 있다. */
    targetSlug?: string | null;
    /** 대상 리소스 유형 */
    targetType?: "NEWS" | "PROJECT" | "FEED" | null;
    /** 수정 시각 */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** HomeBannerDeleteSuccessResponse */
export interface HomeBannerDeleteSuccessResponse {
  /** 삭제 결과 */
  data: {
    /** 삭제한 홈 배너 ID */
    id: number;
  };
  /** 응답 상태 */
  status: string;
}

/** HomeBannerFindAllSuccessResponse */
export interface HomeBannerFindAllSuccessResponse {
  /** 활성 홈 배너 목록 */
  data: {
    /** 배너 ID */
    bannerId: number;
    /** 이동 방식 */
    destinationType: "TARGET" | "URL";
    /** 표시용 이미지 URL */
    imageUrl: string;
    /** URL 유형 */
    linkType?: "INTERNAL_PATH" | "EXTERNAL_URL" | null;
    /** 내부 경로 또는 외부 HTTPS URL */
    linkUrl?: string | null;
    /** 배너 이미지 미디어 ID */
    mediaId: number;
    /** 대상 리소스 ID. 대상이 뉴스·피드일 때만 값이 있다. */
    targetId?: number | null;
    /** 대상 프로젝트 slug. 대상이 프로젝트일 때만 값이 있다. */
    targetSlug?: string | null;
    /** 대상 리소스 유형 */
    targetType?: "NEWS" | "PROJECT" | "FEED" | null;
  }[];
  /** 응답 상태 */
  status: string;
}

/** HomeBannerUpsertRequest */
export interface HomeBannerUpsertRequest {
  /** 즉시 노출 여부 */
  active: boolean;
  /** 이동 방식 */
  destinationType: "TARGET" | "URL";
  /** 0 이상 표시 순서 */
  displayOrder: number;
  /** URL 유형 */
  linkType?: "INTERNAL_PATH" | "EXTERNAL_URL" | null;
  /** 내부 경로 또는 외부 HTTPS URL */
  linkUrl?: string | null;
  /** READY HOME_BANNER 미디어 ID */
  mediaId: number;
  /** 대상 소식·피드 ID. 대상이 소식·피드일 때만 넣는다. */
  targetId?: number | null;
  /** 대상 프로젝트 slug. 대상이 프로젝트일 때만 넣는다. */
  targetSlug?: string | null;
  /** 대상 리소스 유형 */
  targetType?: "NEWS" | "PROJECT" | "FEED" | null;
}

/** HomeStatisticsSuccessResponse */
export interface HomeStatisticsSuccessResponse {
  /** 홈 통계 */
  data: {
    /** 전체 기수 개수 */
    currentCohort: number;
    /** 삭제되지 않은 피드 수 */
    feedCount: number;
    /** 조회 시각에 진행 중인 삭제되지 않은 이벤트 수 */
    ongoingEventCount: number;
    /** 승인되었고 삭제되지 않은 프로젝트 수 */
    projectCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** NewsDeleteSuccessResponse */
export interface NewsDeleteSuccessResponse {
  /** 삭제 결과 */
  data: {
    /** 삭제 시각 */
    deletedAt: string;
    /** 삭제한 소식 ID */
    id: number;
  };
  /** 메타 정보 */
  meta?: object | null;
  /** 응답 상태 */
  status: string;
}

/** NewsFindAllSuccessResponse */
export interface NewsFindAllSuccessResponse {
  /** 소식 목록 */
  data: {
    /** 이벤트 종료 시각. 공지인 경우 null */
    eventEndAt?: string | null;
    /** 이벤트 시작 시각. 공지인 경우 null */
    eventStartAt?: string | null;
    /** 이벤트 상태. 공지인 경우 null */
    eventStatus?: "UPCOMING" | "ONGOING" | "ENDED" | null;
    /** 소식 ID */
    id: number;
    /** 고정 여부 */
    isPinned: boolean;
    /** 좋아요 수 */
    likeCount: number;
    /** 현재 사용자의 좋아요 여부 */
    likedByMe: boolean;
    /** 고정 순서. 고정되지 않은 경우 null */
    pinOrder?: number | null;
    /** 게시 시각 */
    publishedAt: string;
    /** 소식 요약 */
    summary: string;
    /** 소식 제목 */
    title: string;
    /** 소식 유형 */
    type: "NOTICE" | "EVENT";
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 커서. 다음 페이지가 없으면 null */
    nextCursor?: string | null;
    /** 커서와 size를 제외한 유형·이벤트 상태 조건을 만족하는 전체 소식 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** NewsFindDetailSuccessResponse */
export interface NewsFindDetailSuccessResponse {
  /** 소식 상세 */
  data: {
    /** 소식 작성자 */
    author: {
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 이름 */
      name: string;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 소식 본문 */
    body: string;
    /** 소식 CTA */
    cta?: {
      /** CTA 라벨 */
      label: string;
      /** CTA URL */
      url: string;
    };
    /** 이벤트 종료 시각. 공지인 경우 null */
    eventEndAt?: string | null;
    /** 이벤트 시작 시각. 공지인 경우 null */
    eventStartAt?: string | null;
    /** 이벤트 상태. 공지인 경우 null */
    eventStatus?: "UPCOMING" | "ONGOING" | "ENDED" | null;
    /** 소식 ID */
    id: number;
    /** 고정 여부 */
    isPinned: boolean;
    /** 좋아요 수 */
    likeCount: number;
    /** 현재 사용자의 좋아요 여부 */
    likedByMe: boolean;
    /** 다음 소식. 없으면 null */
    next?: {
      /** 다음 소식 ID */
      id: number;
      /** 다음 소식 게시 시각 */
      publishedAt: string;
      /** 다음 소식 제목 */
      title: string;
    };
    /** 고정 순서. 고정되지 않은 경우 null */
    pinOrder?: number | null;
    /** 이전 소식. 없으면 null */
    previous?: {
      /** 이전 소식 ID */
      id: number;
      /** 이전 소식 게시 시각 */
      publishedAt: string;
      /** 이전 소식 제목 */
      title: string;
    };
    /** 게시 시각 */
    publishedAt: string;
    /** 소식 제목 */
    title: string;
    /** 소식 유형 */
    type: "NOTICE" | "EVENT";
  };
  /** 응답 상태 */
  status: string;
}

/** NewsReactionSuccessResponse */
export interface NewsReactionSuccessResponse {
  /** 소식 반응 변경 결과 */
  data: {
    /** 요청한 반응의 활성 상태 */
    active: boolean;
    /** 소식 좋아요 수 */
    likeCount: number;
    /** 소식 ID */
    newsId: number;
    /** 반응 타입. 현재 LIKE만 지원하며 향후 확장할 수 있다. */
    type: string;
  };
  /** 응답 상태 */
  status: string;
}

/** NewsUpdateRequest */
export interface NewsUpdateRequest {
  /** 표시 작성자 이름 */
  authorName: string;
  /** 소식 본문 */
  body: string;
  /** CTA. 삭제할 때 null */
  cta?: {
    /** CTA 라벨 */
    label?: string | null;
    /** CTA URL */
    url?: string | null;
  };
  /** 이벤트 종료 시각. 공지는 null */
  eventEndAt?: string | null;
  /** 이벤트 시작 시각. 공지는 null */
  eventStartAt?: string | null;
  /** 소식 요약 */
  summary: string;
  /** 소식 제목 */
  title: string;
}

/** NewsUpdateSuccessResponse */
export interface NewsUpdateSuccessResponse {
  /** 수정된 소식 */
  data: {
    /** 작성자 */
    author: {
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 이름 */
      name: string;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 소식 본문 */
    body: string;
    /** CTA */
    cta?: {
      /** CTA 라벨 */
      label?: string | null;
      /** CTA URL */
      url?: string | null;
    };
    /** 이벤트 종료 시각 */
    eventEndAt?: string | null;
    /** 이벤트 시작 시각 */
    eventStartAt?: string | null;
    /** 이벤트 상태. 공지는 null */
    eventStatus?: string | null;
    /** 소식 ID */
    id: number;
    /** 고정 여부 */
    isPinned: boolean;
    /** 고정 순서 */
    pinOrder?: number | null;
    /** 게시 시각 */
    publishedAt: string;
    /** 소식 요약 */
    summary: string;
    /** 소식 제목 */
    title: string;
    /** 소식 유형 */
    type: "NOTICE" | "EVENT";
  };
  /** 응답 상태 */
  status: string;
}

/** NoticeCreateRequest */
export interface NoticeCreateRequest {
  /** 공지 작성자 이름 */
  authorName: string;
  /** 공지 본문 */
  body: string;
  /** 공지 CTA */
  cta?: {
    /** CTA 라벨 */
    label: string;
    /** CTA URL */
    url: string;
  };
  /** 공지 요약 */
  summary: string;
  /** 공지 제목 */
  title: string;
}

/** NoticeCreateSuccessResponse */
export interface NoticeCreateSuccessResponse {
  /** 생성된 공지 */
  data: {
    /** 공지 작성자 */
    author: {
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 이름 */
      name: string;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 공지 본문 */
    body: string;
    /** 공지 CTA */
    cta?: {
      /** CTA 라벨 */
      label: string;
      /** CTA URL */
      url: string;
    };
    /** 공지 ID */
    id: number;
    /** 고정 여부 */
    isPinned: boolean;
    /** 고정 순서 */
    pinOrder?: number | null;
    /** 게시 시각 */
    publishedAt: string;
    /** 공지 요약 */
    summary: string;
    /** 공지 제목 */
    title: string;
    /** 소식 유형. 공지 등록이므로 항상 NOTICE다. */
    type: "NOTICE" | "EVENT";
  };
  /** 응답 상태 */
  status: string;
}

/** OAuthSignupRequest */
export interface OAuthSignupRequest {
  /** 프로필 표시 이름 */
  displayName: string;
  /** @[A-Za-z0-9_-]{2,30} 형식의 영구 공개 핸들 */
  handle: string;
}

/** OAuthSignupSuccessResponse */
export interface OAuthSignupSuccessResponse {
  /** 가입 결과 */
  data: {
    /** 생성된 사용자 ID */
    userId: number;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectCommentCreateRequest */
export interface ProjectCommentCreateRequest {
  /** 앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준) */
  content: string;
  /** 같은 프로젝트의 삭제되지 않은 루트 댓글 ID. 없으면 루트 댓글 */
  parentId?: number | null;
}

/** ProjectCommentCreateSuccessResponse */
export interface ProjectCommentCreateSuccessResponse {
  /** 생성된 댓글 */
  data: {
    /** 댓글 작성자 */
    author: {
      /** 작성자 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 저장된 댓글 내용 */
    content: string;
    /** 생성 시각 (UTC ISO-8601) */
    createdAt: string;
    /** 현재 사용자가 수정할 수 있는지 여부 */
    editable: boolean;
    /** 댓글 ID */
    id: number;
    /** 부모 댓글 ID */
    parentId?: number | null;
    /** 수정 시각 (UTC ISO-8601) */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectCommentDeleteSuccessResponse */
export interface ProjectCommentDeleteSuccessResponse {
  /** 삭제된 댓글 */
  data: {
    /** 댓글 삭제 여부 */
    deleted: boolean;
    /** 삭제된 댓글 ID */
    id: number;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectCommentFindAllSuccessResponse */
export interface ProjectCommentFindAllSuccessResponse {
  /** 프로젝트 댓글 목록 */
  data: {
    /** 댓글 공감 수 */
    agreeCount: number;
    /** 현재 사용자의 공감 여부. 비로그인이면 false */
    agreedByMe: boolean;
    /** 댓글 작성자 */
    author: {
      /** 작성자 프로필 이미지 미디어 ID */
      avatarImageId?: number | null;
      /** 작성자 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 댓글 내용. 삭제된 댓글은 null */
    content?: string | null;
    /** 생성 시각 (UTC ISO-8601) */
    createdAt: string;
    /** 댓글이 삭제되었는지 여부 */
    deleted: boolean;
    /** 현재 사용자가 수정할 수 있는지 여부 */
    editable: boolean;
    /** 댓글 내용이 수정된 적이 있는지 여부 */
    edited: boolean;
    /** 댓글 ID */
    id: number;
    /** 부모 루트 댓글 ID */
    parentId?: number | null;
    /** 수정 시각 (UTC ISO-8601) */
    updatedAt: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 cursor */
    nextCursor?: string | null;
    /** 커서와 size에 무관한 삭제되지 않은 루트 댓글과 답글의 전체 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectCommentReactionSuccessResponse */
export interface ProjectCommentReactionSuccessResponse {
  /** 프로젝트 댓글 반응 변경 결과 */
  data: {
    /** 요청한 반응의 활성 상태 */
    active: boolean;
    /** 댓글 공감 수 */
    agreeCount: number;
    /** 댓글 ID */
    commentId: number;
    /** 프로젝트 slug */
    slug: string;
    /** 반응 타입(현재 AGREE) */
    type: string;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectCommentUpdateRequest */
export interface ProjectCommentUpdateRequest {
  /** 앞뒤 공백을 제거한 뒤 저장하는 댓글 내용 (1~500자, Unicode code point 기준) */
  content: string;
}

/** ProjectCommentUpdateSuccessResponse */
export interface ProjectCommentUpdateSuccessResponse {
  /** 수정된 댓글 */
  data: {
    /** 댓글 작성자 */
    author: {
      /** 작성자 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 표시 이름 */
      displayName: string;
      /** 작성자 handle */
      handle?: string | null;
      /** 작성자 트랙 */
      track?: string | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType?: string | null;
    };
    /** 저장된 댓글 내용 */
    content: string;
    /** 생성 시각 (UTC ISO-8601) */
    createdAt: string;
    /** 현재 사용자가 수정할 수 있는지 여부 */
    editable: boolean;
    /** 댓글 내용이 수정된 적이 있는지 여부 */
    edited: boolean;
    /** 댓글 ID */
    id: number;
    /** 부모 댓글 ID */
    parentId?: number | null;
    /** 수정 시각 (UTC ISO-8601, 변경 없으면 기존 값 유지) */
    updatedAt: string;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectCreateRequest */
export interface ProjectCreateRequest {
  /** 우아한테크코스 기수 (1~8) */
  cohort: number;
  /** 서비스 배포 URL (http/https). 빈 문자열이나 공백만 있으면 입력하지 않은 것으로 본다. */
  deploymentUrl?: string | null;
  /** 프로젝트 설명 마크다운 (100,000자 이하). 이미지는 ![설명](media://{mediaId}) 형식으로 넣으며, 본인이 업로드한 PROJECT_DESCRIPTION 용도의 처리 완료 이미지만 쓸 수 있다. */
  descriptionMd?: string | null;
  /** https://github.com/woowacourse-teams/{repo} 형식 (2,048자 이하). 다른 owner의 리포지토리는 400이다. 리포지토리 이름 뒤의 .git, /, 경로(/tree/main), 쿼리(?tab=readme), 앵커(#readme)는 허용하고 버린 뒤 저장한다. 리포지토리 이름으로 slug를 만든다. */
  githubRepositoryUrl: string;
  /** @[A-Za-z0-9_-]{2,30} 형식의 팀원 handle 목록 (1명 이상). 등록자를 포함할 수 있으며, 활동 중인 우아한테크코스 크루 또는 코치여야 하며, 대소문자만 다른 handle도 같은 사용자로 본다. 배열 순서가 표시 순서가 된다. 각 handle의 앞뒤 공백은 자르며, 공백만 있는 handle은 400이다. */
  memberHandles: string[];
  /** 한 줄 소개. 앞뒤 공백을 자른 뒤 200자 이하 (유니코드 코드 포인트 기준) */
  tagline: string;
  /** 팀 이름. 앞뒤 공백을 자른 뒤 50자 이하 (유니코드 코드 포인트 기준) */
  teamName: string;
  /** 선택 가능한 기술 스택 ID 목록. 중복할 수 없으며, 배열 순서가 표시 순서가 된다. */
  techTagIds: number[];
  /** 본인이 업로드한 PROJECT_THUMBNAIL 용도의 처리 완료 이미지 ID */
  thumbnailImageId?: number | null;
  /** 프로젝트 이름. 앞뒤 공백을 자른 뒤 100자 이하 (유니코드 코드 포인트 기준) */
  title: string;
}

/** ProjectCreateSuccessResponse */
export interface ProjectCreateSuccessResponse {
  /** 등록된 프로젝트 */
  data: {
    /** 프로젝트 주소로 쓰이는 slug */
    slug: string;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectDeleteSuccessResponse */
export interface ProjectDeleteSuccessResponse {
  /** 삭제 결과 */
  data: {
    /** 삭제 시각 (UTC) */
    deletedAt: string;
    /** 복구 기한 (UTC). 이 시각까지 복구할 수 있다. */
    restoreDeadlineAt: string;
    /** 삭제한 프로젝트 slug */
    slug: string;
  };
  /** 메타 정보 */
  meta?: object | null;
  /** 응답 상태 */
  status: string;
}

/** ProjectFilterOptionsSuccessResponse */
export interface ProjectFilterOptionsSuccessResponse {
  /** 필터 옵션 */
  data: {
    /** 전체 기수. 최신 기수부터 정렬하며, 프로젝트 수가 0인 기수도 포함한다. */
    cohorts: {
      /** 우아한테크코스 기수 */
      cohort: number;
      /** 검색어와 기술 스택 조건에 맞는 그 기수의 프로젝트 수. 다른 기수를 골라도 0이 되지 않는다. */
      projectCount: number;
      /** 기수 연도 */
      year: number;
    }[];
    /** 검색어와 고른 필터를 모두 적용한 프로젝트 수. 같은 조건의 목록 조회 meta.totalCount와 같다. */
    matchedProjectCount: number;
    /** 활성 기술 스택 전체. 이름순으로 정렬하며, 프로젝트 수가 0인 기술 스택도 포함한다. */
    techTags: {
      /** 기술 스택 이름 */
      displayName: string;
      /** 기술 스택 ID */
      id: number;
      /** 현재 조건에 이 기술 스택을 추가로 골랐을 때의 프로젝트 수. 이미 고른 기술 스택은 matchedProjectCount와 같다. */
      projectCount: number;
    }[];
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectFindAllSuccessResponse */
export interface ProjectFindAllSuccessResponse {
  /** 프로젝트 목록 */
  data: {
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부. 비로그인이면 false다. */
    bookmarkedByMe: boolean;
    /** 우아한테크코스 기수 */
    cohort: number;
    /** 삭제되지 않은 댓글 수 (대댓글 포함) */
    commentCount: number;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부. 비로그인이면 false다. */
    likedByMe: boolean;
    /** 팀원 전체 목록. 상세 조회의 members와 같은 규칙이며, 등록 순서대로 정렬한다. */
    members: {
      /** 프로필 이미지 URL. 가입한 팀원은 CloudFront에서 제공하는 공개 이미지 URL, 가입하지 않은 이관 팀원은 GitHub 프로필 이미지 URL이다. */
      avatarUrl?: string | null;
      /** 기수. 크루가 아닌 팀원과 이관 팀원은 null이다. */
      cohort?: number | null;
      /** 표시 이름. 탈퇴한 팀원은 '탈퇴한 사용자', 가입하지 않은 이관 팀원은 GitHub 이름(없으면 GitHub 아이디)이다. */
      displayName: string;
      /** GitHub 프로필 URL. 가입하지 않은 이관 팀원만 값이 있다. */
      githubProfileUrl?: string | null;
      /** 프로필 페이지 이동용 handle. 가입하지 않은 이관 팀원은 null이다. */
      handle?: string | null;
      /** 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 사용자 ID. 가입하지 않은 이관 팀원은 null이다. */
      userId?: number | null;
      /** 사용자 유형. 가입하지 않은 이관 팀원과 탈퇴한 팀원은 null이다. */
      userType?: string | null;
    }[];
    /** 프로젝트 주소로 쓰이는 slug */
    slug: string;
    /** GitHub star 수. 동기화 전이면 null이다. */
    starCount?: number | null;
    /** 한 줄 소개 */
    tagline: string;
    /** 기술 스택 전체 목록. 등록 순서대로 정렬하며, 카드에 몇 개까지 보여줄지는 화면에서 정한다. */
    techTags: {
      /** 기술 스택 이름 */
      displayName: string;
      /** 기술 스택 ID */
      id: number;
    }[];
    /** 프로젝트 썸네일 미디어 ID */
    thumbnailImageId?: number | null;
    /** CloudFront에서 제공하는 공개 썸네일 URL */
    thumbnailUrl?: string | null;
    /** 프로젝트 이름 */
    title: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 조회에 쓸 커서. 다음 페이지가 없으면 null */
    nextCursor?: string | null;
    /** 커서와 size를 제외한 검색어와 필터 조건을 만족하는 전체 프로젝트 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectFindDetailSuccessResponse */
export interface ProjectFindDetailSuccessResponse {
  /** 프로젝트 상세 */
  data: {
    /** 승인 상태 */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부. 비로그인이면 false다. */
    bookmarkedByMe: boolean;
    /** 우아한테크코스 기수 */
    cohort: number;
    /** 삭제되지 않은 댓글 수 (대댓글 포함) */
    commentCount: number;
    /** 등록 시각 */
    createdAt: string;
    /** 서비스 배포 URL */
    deploymentUrl?: string | null;
    /** 프로젝트 설명 마크다운. 본문 이미지 참조는 공개 URL로 변환되어 있다. */
    descriptionMd?: string | null;
    /** 본문 이미지의 미디어 ID와 공개 URL 매핑 */
    descriptionMedia: {
      /** 본문 이미지 미디어 ID */
      mediaId: number;
      /** 본문 이미지 공개 URL */
      url: string;
    }[];
    /** 요청자가 등록자 본인인지 여부. 수정·삭제할 수 있는 사용자에게만 true다. 비로그인이거나 이전 기수에서 이관된 프로젝트면 false다. */
    editable: boolean;
    /** GitHub 리포지토리 URL */
    githubRepositoryUrl: string;
    /** CloudFront에서 제공하는 공개 이미지 URL */
    imageUrl?: string | null;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부. 비로그인이면 false다. */
    likedByMe: boolean;
    /** 팀원 목록. 저장된 순서대로 반환한다. */
    members: {
      /** 프로필 이미지 URL. 가입한 팀원은 CloudFront에서 제공하는 공개 이미지 URL, 가입하지 않은 이관 팀원은 GitHub 프로필 이미지 URL이다. */
      avatarUrl?: string | null;
      /** 기수. 크루가 아닌 팀원과 이관 팀원은 null이다. */
      cohort?: number | null;
      /** 표시 이름. 탈퇴한 팀원은 '탈퇴한 사용자', 가입하지 않은 이관 팀원은 GitHub 이름(없으면 GitHub 아이디)이다. */
      displayName: string;
      /** GitHub 프로필 URL. 가입하지 않은 이관 팀원만 값이 있다. */
      githubProfileUrl?: string | null;
      /** 프로필 페이지 이동용 handle. 가입하지 않은 이관 팀원은 null이다. */
      handle?: string | null;
      /** 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 사용자 ID. 가입하지 않은 이관 팀원은 null이다. */
      userId?: number | null;
      /** 사용자 유형. 가입하지 않은 이관 팀원과 탈퇴한 팀원은 null이다. */
      userType?: string | null;
    }[];
    /** 반려 사유. REJECTED일 때만 값이 있고 그 외에는 null이다. */
    rejectReason?: string | null;
    /** 운영 상태 */
    serviceStatus: "OPERATING" | "CLOSED";
    /** 프로젝트 주소로 쓰이는 slug */
    slug: string;
    /** GitHub star 수. 동기화 전이면 null이다. */
    starCount?: number | null;
    /** 한 줄 소개 */
    tagline: string;
    /** 팀 이름 */
    teamName: string;
    /** 기술 스택 목록. 등록 순서대로 정렬한다. */
    techTags: {
      /** 기술 스택 이름 */
      displayName: string;
      /** 기술 스택 ID */
      id: number;
    }[];
    /** 프로젝트 썸네일 미디어 ID */
    thumbnailImageId?: number | null;
    /** 프로젝트 이름 */
    title: string;
    /** 수정 시각 */
    updatedAt: string;
    /** 조회수 */
    viewCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectReactionSuccessResponse */
export interface ProjectReactionSuccessResponse {
  /** 프로젝트 반응 변경 결과 */
  data: {
    /** 요청한 반응의 활성 상태 */
    active: boolean;
    /** 프로젝트 북마크 수 */
    bookmarkCount: number;
    /** 프로젝트 좋아요 수 */
    likeCount: number;
    /** 프로젝트 slug */
    slug: string;
    /** 반응 타입(LIKE 또는 BOOKMARK) */
    type: string;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectRestoreSuccessResponse */
export interface ProjectRestoreSuccessResponse {
  /** 복구 결과 */
  data: {
    /** 승인 상태. 삭제 이전 값을 그대로 유지한다. */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 복구 시각 (UTC) */
    restoredAt: string;
    /** 복구한 프로젝트 slug */
    slug: string;
  };
  /** 메타 정보 */
  meta?: object | null;
  /** 응답 상태 */
  status: string;
}

/** ProjectUpdateRequest */
export interface ProjectUpdateRequest {
  /** 우아한테크코스 기수 (1~8) */
  cohort: number;
  /** 서비스 배포 URL (http/https). 비우려면 null로 보낸다. 빈 문자열이나 공백만 있어도 비운 것으로 본다. */
  deploymentUrl?: string | null;
  /** 프로젝트 설명 마크다운 (100,000자 이하). 이미지는 ![설명](media://{mediaId}) 형식으로 넣으며, 상세 조회 응답의 CDN URL을 그대로 보내도 기존 본문 이미지 참조를 유지한다. */
  descriptionMd?: string | null;
  /** https://github.com/woowacourse-teams/{repo} 형식 (2,048자 이하). 다른 owner의 리포지토리는 400이다. 리포지토리 이름 뒤의 .git, /, 경로(/tree/main), 쿼리(?tab=readme), 앵커(#readme)는 허용하고 버린 뒤 저장한다. 바꿀 수 있지만 다른 프로젝트가 등록한 리포지토리로는 바꿀 수 없다. slug는 등록 시점 값으로 고정이라 따라 바뀌지 않는다. */
  githubRepositoryUrl: string;
  /** 팀원 handle 전체 목록 (1명 이상). 등록자를 포함할 수 있으며, 통째로 교체하며 배열 순서가 표시 순서가 된다. 이미 팀원인 사용자는 탈퇴했어도 그대로 둘 수 있다. 각 handle의 앞뒤 공백은 자르며, 공백만 있는 handle은 400이다. */
  memberHandles: string[];
  /** 서비스 운영 상태. deploymentUrl이 없으면 CLOSED만 보낼 수 있으며, 어기면 400을 반환한다. 이때 오류 응답의 details.field는 serviceStatusValid다. */
  serviceStatus: "OPERATING" | "CLOSED";
  /** 한 줄 소개. 앞뒤 공백을 자른 뒤 200자 이하 (유니코드 코드 포인트 기준) */
  tagline: string;
  /** 팀 이름. 앞뒤 공백을 자른 뒤 50자 이하 (유니코드 코드 포인트 기준) */
  teamName: string;
  /** 기술 스택 ID 전체 목록. 통째로 교체하며 배열 순서가 표시 순서가 된다. 이미 달려 있던 태그는 비활성화됐어도 그대로 둘 수 있다. */
  techTagIds: number[];
  /** 본인이 업로드한 PROJECT_THUMBNAIL 용도의 처리 완료 이미지 ID. 필드를 생략하면 기존 썸네일을 유지하고, null을 보내면 제거한다. */
  thumbnailImageId?: number | null;
  /** 프로젝트 이름. 앞뒤 공백을 자른 뒤 100자 이하 (유니코드 코드 포인트 기준) */
  title: string;
}

/** ProjectUpdateSuccessResponse */
export interface ProjectUpdateSuccessResponse {
  /** 수정 결과 */
  data: {
    /** 수정 후 승인 상태. PENDING 또는 APPROVED이며 REJECTED는 오지 않는다. */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 수정한 프로젝트 slug */
    slug: string;
  };
  /** 응답 상태 */
  status: string;
}

/** ProjectViewRecordSuccessResponse */
export interface ProjectViewRecordSuccessResponse {
  /** 조회 기록 결과 */
  data: {
    /** 이번 조회를 반영한 조회수. 같은 날 다시 조회해 집계되지 않았으면 현재 조회수다. */
    viewCount: number;
  };
  /** 메타 정보 */
  meta?: object | null;
  /** 응답 상태 */
  status: string;
}

/** TechTagFindAllSuccessResponse */
export interface TechTagFindAllSuccessResponse {
  /** 기술 스택 목록 */
  data: {
    /** 기술 스택 목록 */
    items: {
      /** 화면 표시 이름 */
      displayName: string;
      /** 기술 스택 ID */
      id: number;
    }[];
  };
  /** 응답 상태 */
  status: string;
}

/** UserCommentFindAllSuccessResponse */
export interface UserCommentFindAllSuccessResponse {
  /** 내가 작성한 댓글 목록 */
  data: {
    /** 댓글 공감 수 */
    agreeCount: number;
    /** 현재 사용자의 공감 여부 */
    agreedByMe: boolean;
    /** 댓글 ID */
    commentId: number;
    /** 댓글 내용 */
    content: string;
    /** 댓글 작성 시각 */
    createdAt: string;
    /** 피드 댓글이면 이동할 피드 ID. 프로젝트 댓글이면 null */
    feedId?: number | null;
    /** 프로젝트 댓글이면 이동할 프로젝트 slug. 피드 댓글이면 null */
    projectSlug?: string | null;
    /** 댓글 대상 종류 */
    type: "FEED" | "PROJECT";
    /** 댓글 최종 수정 시각 */
    updatedAt: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 조회용 커서 */
    nextCursor?: string | null;
    /** 커서와 size에 무관한 조회 가능한 전체 작성 댓글 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** UserFeedFindAllSuccessResponse */
export interface UserFeedFindAllSuccessResponse {
  /** 사용자가 작성한 피드 목록 */
  data: {
    /** 현재 작성자 프로필 */
    author: {
      /** 현재 프로필 이미지 미디어 ID */
      avatarImageId?: number | null;
      /** 현재 프로필 이미지 공개 URL */
      avatarUrl?: string | null;
      /** 작성자 기수 */
      cohort?: number | null;
      /** 작성자 이름 */
      displayName: string;
      /** 작성자 핸들 */
      handle: string;
      /** 작성자 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 작성자 ID */
      userId: number;
      /** 작성자 유형 */
      userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
    };
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부. 비로그인이면 false */
    bookmarkedByMe: boolean;
    /** 카테고리 목록 */
    categories: {
      /** 카테고리 ID */
      categoryId: number;
      /** 카테고리 표시 이름 */
      displayName: string;
      /** 카테고리 slug */
      slug: string;
      /** 카테고리 유형 */
      type: "GENERAL" | "EVENT";
    }[];
    /** 삭제되지 않은 댓글 수 */
    commentCount: number;
    /** Markdown 본문 */
    content: string;
    /** ISO-8601 생성 시각 */
    createdAt: string;
    /** 피드 ID */
    feedId: number;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부. 비로그인이면 false */
    likedByMe: boolean;
    /** 본문 미디어 목록 */
    media: {
      /** 미디어 표시 순서 */
      displayOrder: number;
      /** 본문 미디어 ID */
      mediaId: number;
      /** 본문 미디어 공개 URL */
      url: string;
    }[];
    /** 피드 제목 */
    title: string;
    /** ISO-8601 수정 시각 */
    updatedAt: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 커서 */
    nextCursor?: string | null;
    /** 커서와 size에 무관한 해당 사용자의 전체 공개 피드 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** UserProfileSuccessResponse */
export interface UserProfileSuccessResponse {
  /** 사용자 프로필 */
  data: {
    /** 프로필 이미지 미디어 ID */
    avatarImageId?: number | null;
    /** 프로필 이미지 공개 URL */
    avatarUrl?: string | null;
    /** 한 줄 소개 */
    bio?: string | null;
    /** 블로그 URL */
    blogUrl?: string | null;
    /** 우테코 기수 */
    cohort?: number | null;
    /** 프로필 항목 개수 */
    counts: {
      /** 삭제되지 않은 작성 피드 개수 */
      feeds: number;
      /** 삭제되지 않은 참여 프로젝트 개수 */
      projects: number;
    };
    /** 표시 이름 */
    displayName: string;
    /** GitHub 프로필 URL */
    githubProfileUrl?: string | null;
    /** 사용자 handle */
    handle: string;
    /** 우테코 트랙 */
    track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
    /** 사용자 ID */
    userId: number;
    /** 사용자 유형 */
    userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
  };
  /** 응답 상태 */
  status: string;
}

/** UserProfileSummarySuccessResponse */
export interface UserProfileSummarySuccessResponse {
  /** 프로필 요약 정보 */
  data: {
    /** 프로필 이미지 미디어 ID */
    avatarImageId?: number | null;
    /** 프로필 이미지 공개 URL */
    avatarUrl?: string | null;
    /** 표시 이름 */
    displayName: string;
    /** 사용자 handle */
    handle: string;
  };
  /** 응답 상태 */
  status: string;
}

/** UserProfileUpdateRequest */
export interface UserProfileUpdateRequest {
  /** READY 상태의 USER_AVATAR 미디어 ID */
  avatarImageId?: number | null;
  /** 한 줄 소개 */
  bio?: string | null;
  /** 블로그 URL */
  blogUrl?: string | null;
  /** 표시 이름. 인증된 우테코 사용자는 변경 불가 */
  displayName: string;
  /** GitHub 프로필 URL */
  githubProfileUrl?: string | null;
}

/** UserProfileUpdateSuccessResponse */
export interface UserProfileUpdateSuccessResponse {
  /** 수정된 사용자 프로필 */
  data: {
    /** 프로필 이미지 공개 URL */
    avatarUrl?: string | null;
    /** 한 줄 소개 */
    bio?: string | null;
    /** 블로그 URL */
    blogUrl?: string | null;
    /** 우테코 기수 */
    cohort?: number | null;
    /** 표시 이름 */
    displayName: string;
    /** GitHub 프로필 URL */
    githubProfileUrl?: string | null;
    /** 사용자 handle */
    handle: string;
    /** 우테코 트랙 */
    track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
    /** 사용자 ID */
    userId: number;
    /** 사용자 유형 */
    userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
  };
  /** 응답 상태 */
  status: string;
}

/** UserProjectFindAllSuccessResponse */
export interface UserProjectFindAllSuccessResponse {
  /** 사용자가 참여한 프로젝트 목록 */
  data: {
    /** 심사 상태 */
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    /** 북마크 수 */
    bookmarkCount: number;
    /** 요청자의 북마크 여부. 비로그인이면 false다. */
    bookmarkedByMe: boolean;
    /** 우아한테크코스 기수 */
    cohort: number;
    /** 삭제되지 않은 댓글 수 */
    commentCount: number;
    /** 좋아요 수 */
    likeCount: number;
    /** 요청자의 좋아요 여부. 비로그인이면 false다. */
    likedByMe: boolean;
    /** 프로젝트 팀원 */
    members: {
      /** 프로필 이미지 URL. 가입한 팀원은 CloudFront에서 제공하는 공개 이미지 URL, 가입하지 않은 이관 팀원은 GitHub 프로필 이미지 URL이다. */
      avatarUrl?: string | null;
      /** 기수 */
      cohort?: number | null;
      /** 표시 이름 */
      displayName: string;
      /** 이관 팀원의 GitHub 프로필 URL */
      githubProfileUrl?: string | null;
      /** 사용자 handle */
      handle?: string | null;
      /** 트랙 */
      track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
      /** 사용자 ID. 이관 팀원은 null이다. */
      userId?: number | null;
      /** 사용자 유형 */
      userType?: string | null;
    }[];
    /** 반려 사유. REJECTED일 때만 값이 있고 그 외에는 null이다. */
    rejectReason?: string | null;
    /** 운영 상태 */
    serviceStatus: "OPERATING" | "CLOSED";
    /** 프로젝트 slug */
    slug: string;
    /** GitHub star 수. 동기화 전이면 null이다. */
    starCount?: number | null;
    /** 한 줄 소개 */
    tagline: string;
    /** 팀 이름 */
    teamName: string;
    /** 기술 스택 */
    techTags: {
      /** 기술 스택 이름 */
      displayName: string;
      /** 기술 스택 ID */
      id: number;
    }[];
    /** 프로젝트 썸네일 이미지 ID */
    thumbnailImageId?: number | null;
    /** CloudFront에서 제공하는 공개 썸네일 URL */
    thumbnailUrl?: string | null;
    /** 프로젝트 이름 */
    title: string;
  }[];
  /** 페이지네이션 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 커서 */
    nextCursor?: string | null;
    /** 커서와 size에 무관한 조회 가능한 전체 참여 프로젝트 수. 본인 조회는 승인 대기 및 반려 프로젝트를 포함하고, 타인 또는 비로그인 조회는 승인된 프로젝트만 포함 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** UserSearchSuccessResponse */
export interface UserSearchSuccessResponse {
  /** 검색된 크루와 코치 */
  data: {
    /** 프로필 이미지 미디어 ID */
    avatarImageId?: number | null;
    /** 프로필 이미지 공개 URL */
    avatarUrl?: string | null;
    /** 우테코 기수 */
    cohort?: number | null;
    /** 표시 이름 */
    displayName: string;
    /** 사용자 handle */
    handle: string;
    /** 우테코 트랙 */
    track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
    /** 사용자 ID */
    userId: number;
    /** 사용자 유형 */
    userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
  }[];
  /** 페이지 정보 */
  meta: {
    /** 다음 페이지 존재 여부 */
    hasNext: boolean;
    /** 다음 페이지 커서 */
    nextCursor?: string | null;
    /** 커서와 size를 제외한 검색 조건을 만족하는 전체 사용자 수 */
    totalCount: number;
  };
  /** 응답 상태 */
  status: string;
}

/** UserVerificationRequestCreateRequest */
export interface UserVerificationRequestCreateRequest {
  /** 크루 신청 시 기수. 양의 정수이며 실제 유효성은 관리자 확인 */
  cohort?: number | null;
  /** 우테코 닉네임(앞뒤 공백 제거 후 50자 이하) */
  nickname: string;
  /** 크루 신청 시 트랙 */
  track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
  /** 신청 유형. WOOWACOURSE_CREW 또는 WOOWACOURSE_COACH만 보낼 수 있다. */
  userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
}

/** UserVerificationRequestCreateSuccessResponse */
export interface UserVerificationRequestCreateSuccessResponse {
  /** 생성된 인증 신청 */
  data: {
    /** 신청 기수 */
    cohort?: number | null;
    /** 신청 닉네임 */
    nickname: string;
    /** 인증 신청 ID */
    requestId: number;
    /** 신청 시각(ISO-8601) */
    requestedAt: string;
    /** 신청 상태 */
    status: "PENDING" | "APPROVED" | "REJECTED";
    /** 신청 트랙 */
    track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
    /** 신청 유형 */
    userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
  };
  /** 응답 상태 */
  status: string;
}

/** UserVerificationRequestSuccessResponse */
export interface UserVerificationRequestSuccessResponse {
  /** 최신 인증 신청 */
  data?: {
    /** 신청 기수. 코치 신청은 null */
    cohort?: number | null;
    /** 승인/반려 시각(ISO-8601). PENDING이면 null */
    decidedAt?: string | null;
    /** 신청 닉네임 */
    nickname: string;
    /** 반려 사유. REJECTED일 때만 반환 */
    reason?: string | null;
    /** 인증 신청 ID. 기존 인증 사용자는 null */
    requestId?: number | null;
    /** 신청 시각(ISO-8601). 기존 인증 사용자는 null */
    requestedAt?: string | null;
    /** 신청 상태 */
    status: "PENDING" | "APPROVED" | "REJECTED";
    /** 신청 트랙. 코치 신청은 null */
    track?: "BACKEND" | "ANDROID" | "FRONTEND" | null;
    /** 신청 유형 */
    userType: "GENERAL" | "WOOWACOURSE_CREW" | "WOOWACOURSE_COACH";
  };
  /** 응답 상태 */
  status: string;
}
