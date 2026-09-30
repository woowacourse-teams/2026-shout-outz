import ky from 'ky';
import type { MediaStatusSuccessResponse } from '@/api/generated/schema';
import { kyInstance } from '@/utils/http';

const MEDIA_PATH = '/api/v1/media';

/**
 * 업로드 1단계에 보내는 용도. 서버가 용도별로 크기·형식을 검사한다.
 *
 * 미디어 API는 서버 문서(openapi3.yaml)에 엔드포인트째로 없다. 이 목록은 **서버가 준 타입이 아니라**
 * 다른 필드의 설명문에서 모아 손으로 적은 것이다. 서버가 값을 늘려도 여기서는 알 수 없고,
 * 틀린 값을 보내면 컴파일이 아니라 런타임 400(`invalid-purpose`)으로만 드러난다.
 *
 * 출처(schema.ts의 설명문):
 * - `FEED_CONTENT`        FeedSaveRequest.mediaIds
 * - `HOME_BANNER`         HomeBannerUpsertRequest.mediaId
 * - `PROJECT_DESCRIPTION` ProjectCreateRequest.descriptionMd
 * - `PROJECT_THUMBNAIL`   ProjectCreateRequest.thumbnailImageId
 * - `USER_AVATAR`         UserProfileUpdateRequest.avatarImageId
 */
export type MediaPurpose =
  'FEED_CONTENT' | 'HOME_BANNER' | 'PROJECT_DESCRIPTION' | 'PROJECT_THUMBNAIL' | 'USER_AVATAR';

interface MediaUploadTicket {
  mediaId: number;
  uploadUrl: string;
  contentType: string;
}

/**
 * 이미지를 올리고 `mediaId`를 돌려준다.
 *
 * 세 단계를 한 번에 처리한다.
 * 1. `POST /api/v1/media/uploads`로 presigned PUT URL 발급
 * 2. 발급받은 URL로 스토리지에 파일 직접 PUT
 * 3. `POST /api/v1/media/{mediaId}/complete`로 업로드 완료 통보
 *
 * 아직 대상이 만들어지기 전(프로젝트 등록, 배너 등록, 프로필 수정)에도 올릴 수 있어야 해서
 * 1번의 targetId를 null로 보낸다(백엔드와 합의됨).
 */

/**
 * 서버가 밝힌 이미지 처리 시간은 최대 1분이다. 지연을 감안해 조금 더 잡는다.
 *
 * 처리 동시성이 2라 대기 시간이 내 이미지 크기가 아니라 앞사람들의 큐 길이에 좌우된다.
 */
const READY_TIMEOUT_MS = 70_000;

/** 이만큼 지나면 오래 걸리고 있다고 알린다. 기다리는 것은 계속한다. */
const SLOW_NOTICE_MS = 10_000;

const MIN_POLL_DELAY_MS = 200;
const MAX_POLL_DELAY_MS = 3_000;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 기다리다 끊긴 경우. 업로드는 끝났고 서버가 아직 처리 중이다.
 *
 * `mediaId`를 들고 있으므로 파일을 다시 올리지 않고 `waitUntilReady`만 다시 부르면 된다.
 */
export class MediaNotReadyError extends Error {
  constructor(readonly mediaId: number) {
    super('이미지 처리가 아직 끝나지 않았습니다.');
    this.name = 'MediaNotReadyError';
  }
}

/** 더 기다려도 바뀌지 않는 상태로 끝난 경우. */
export class MediaProcessingFailedError extends Error {
  constructor(readonly status: 'FAILED' | 'EXPIRED') {
    super(`이미지 처리에 실패했습니다: ${status}`);
    this.name = 'MediaProcessingFailedError';
  }
}

export interface WaitUntilReadyOptions {
  /** 오래 걸릴 때 한 번 불린다. 화면이 안내 문구로 바꿀 수 있게 한다. */
  onSlow?: () => void;
  /** @default 70_000 */
  timeoutMs?: number;
  /** @default 10_000 */
  slowNoticeMs?: number;
}

/**
 * 이미지 처리가 끝날 때까지 기다린다.
 *
 * `complete`는 처리를 큐에 넣고 바로 응답한다(서버가 별도 스레드 풀에서 돌린다). 그래서 응답이
 * 왔다고 쓸 수 있는 게 아니고, 이 상태로 프로필을 저장하면 `AVATAR_IMAGE_NOT_READY`로 거절된다.
 *
 * 저장을 반복하는 대신 상태만 물어본다. 실패한 저장은 전체 프로필 교체 PUT이라 무겁고,
 * 무엇보다 `FAILED`와 "아직 처리 중"을 구분하지 못한다.
 */
export async function waitUntilReady(
  mediaId: number,
  {
    onSlow,
    timeoutMs = READY_TIMEOUT_MS,
    slowNoticeMs = SLOW_NOTICE_MS,
  }: WaitUntilReadyOptions = {},
): Promise<void> {
  const path = `${MEDIA_PATH}/${mediaId}/status`;
  const startedAt = Date.now();
  let notified = false;
  let delayMs = MIN_POLL_DELAY_MS;

  for (;;) {
    const body = await kyInstance.get(path).json<MediaStatusSuccessResponse>();
    const { status } = body.data;

    if (status === 'READY') return;
    if (status === 'FAILED' || status === 'EXPIRED') throw new MediaProcessingFailedError(status);

    const elapsedMs = Date.now() - startedAt;
    if (elapsedMs >= timeoutMs) throw new MediaNotReadyError(mediaId);

    if (!notified && elapsedMs >= slowNoticeMs) {
      notified = true;
      onSlow?.();
    }

    await sleep(delayMs);
    delayMs = Math.min(delayMs * 2, MAX_POLL_DELAY_MS);
  }
}

export async function uploadMedia(
  file: File,
  purpose: MediaPurpose,
  options?: WaitUntilReadyOptions,
): Promise<number> {
  const ticket = await kyInstance
    .post(`${MEDIA_PATH}/uploads`, {
      json: {
        purpose,
        targetId: null,
        originalFileName: file.name,
        contentType: file.type,
        sizeBytes: file.size,
      },
    })
    .json<MediaUploadTicket>();

  // presigned URL은 우리 서버가 아니라 스토리지로 가므로 세션 쿠키와 CSRF 헤더를 붙이지 않는다.
  await ky.put(ticket.uploadUrl, {
    body: file,
    headers: { 'Content-Type': ticket.contentType },
  });

  await kyInstance.post(`${MEDIA_PATH}/${ticket.mediaId}/complete`);
  await waitUntilReady(ticket.mediaId, options);

  return ticket.mediaId;
}

export const uploadProjectThumbnail = (file: File) => uploadMedia(file, 'PROJECT_THUMBNAIL');

export const uploadHomeBannerImage = (file: File) => uploadMedia(file, 'HOME_BANNER');

export const uploadAvatar = (file: File, options?: WaitUntilReadyOptions) =>
  uploadMedia(file, 'USER_AVATAR', options);
