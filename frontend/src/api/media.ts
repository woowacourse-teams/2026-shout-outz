/**
 * 프로젝트 썸네일 이미지를 올리고 `mediaId`를 돌려준다.
 *
 * 세 단계를 한 번에 처리한다.
 * 1. `POST /api/v1/media/uploads`로 presigned PUT URL 발급
 * 2. 발급받은 URL로 스토리지에 파일 직접 PUT
 * 3. `POST /api/v1/media/{mediaId}/complete`로 업로드 완료 통보
 *
 * 등록 화면에서는 아직 프로젝트가 없어 1번의 targetId를 null로 보낸다(백엔드와 합의됨).
 */
export async function uploadProjectThumbnail(file: File): Promise<number> {
  throw new Error('uploadProjectThumbnail은 아직 구현되지 않았습니다.');
}
