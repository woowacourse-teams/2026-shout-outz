/**
 * 대표 썸네일 이미지 필드(16:9 권장).
 *
 * 파일을 고르면 바로 업로드해서 받은 `mediaId`를 `onChange`로 알린다. 업로드 중에는 진행 상태를,
 * 실패하면 오류 문구를 보여준다.
 */
export interface ThumbnailFieldProps {
  value: number | null;
  onChange: (next: number | null) => void;
  error?: string;
}

export function ThumbnailField(props: ThumbnailFieldProps) {
  return null;
}
