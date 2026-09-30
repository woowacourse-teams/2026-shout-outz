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

/** 상태를 물어보는 간격. 다 쓰면 포기한다. 합이 약 9초다. */
const STATUS_POLL_DELAYS_MS = [200, 300, 500, 800, 1200, 2000, 2000, 2000];

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 이미지 처리가 끝날 때까지 기다린다.
 *
 * `complete`는 처리를 큐에 넣고 바로 응답한다(서버가 별도 스레드 풀에서 돌린다). 그래서 응답이
 * 왔다고 쓸 수 있는 게 아니고, 이 상태로 프로필을 저장하면 `AVATAR_IMAGE_NOT_READY`로 거절된다.
 *
 * 저장 요청을 반복하는 대신 상태만 물어본다. 실패한 저장은 전체 프로필 교체 PUT이라 무겁고,
 * 무엇보다 `FAILED`와 "아직 처리 중"을 구분하지 못한다.
 */
async function waitUntilReady(mediaId: number): Promise<void> {
  const path = `${MEDIA_PATH}/${mediaId}/status`;

  for (let attempt = 0; ; attempt += 1) {
    const body = await kyInstance.get(path).json<MediaStatusSuccessResponse>();
    const { status } = body.data;

    if (status === 'READY') return;
    // 처리가 끝나 버린 상태들. 더 기다려도 바뀌지 않는다.
    if (status === 'FAILED' || status === 'EXPIRED') {
      throw new Error(`이미지 처리에 실패했습니다: ${status}`);
    }

    const delayMs = STATUS_POLL_DELAYS_MS[attempt];
    if (delayMs === undefined) throw new Error('이미지 처리가 끝나지 않았습니다.');

    await sleep(delayMs);
  }
}

export async function uploadMedia(file: File, purpose: MediaPurpose): Promise<number> {
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
  await waitUntilReady(ticket.mediaId);

  return ticket.mediaId;
}

export const uploadProjectThumbnail = (file: File) => uploadMedia(file, 'PROJECT_THUMBNAIL');

export const uploadHomeBannerImage = (file: File) => uploadMedia(file, 'HOME_BANNER');

export const uploadAvatar = (file: File) => uploadMedia(file, 'USER_AVATAR');
