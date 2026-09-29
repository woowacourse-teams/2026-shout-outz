import ky from 'ky';
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

  return ticket.mediaId;
}

export const uploadProjectThumbnail = (file: File) => uploadMedia(file, 'PROJECT_THUMBNAIL');

export const uploadHomeBannerImage = (file: File) => uploadMedia(file, 'HOME_BANNER');

export const uploadAvatar = (file: File) => uploadMedia(file, 'USER_AVATAR');
