import ky from 'ky';
import { kyInstance } from '@/utils/http';

const MEDIA_PATH = '/api/v1/media';

/** 미디어 API는 다른 API와 달리 {status, data} 봉투 없이 그대로 내려준다. */
interface MediaUploadTicket {
  mediaId: number;
  uploadUrl: string;
  contentType: string;
}

/** 업로드 1단계에 보내는 용도. 서버가 용도별로 크기·형식을 검사한다. */
export type MediaPurpose = 'PROJECT_THUMBNAIL' | 'HOME_BANNER';

/**
 * 이미지를 올리고 `mediaId`를 돌려준다.
 *
 * 세 단계를 한 번에 처리한다.
 * 1. `POST /api/v1/media/uploads`로 presigned PUT URL 발급
 * 2. 발급받은 URL로 스토리지에 파일 직접 PUT
 * 3. `POST /api/v1/media/{mediaId}/complete`로 업로드 완료 통보
 *
 * 등록 화면에서는 아직 대상이 없어 1번의 targetId를 null로 보낸다(백엔드와 합의됨).
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
