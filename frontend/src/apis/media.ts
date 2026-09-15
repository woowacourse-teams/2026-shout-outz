import { queryOptions } from '@tanstack/react-query';

import { kyInstance } from '@/utils/http';

interface MediaResponse {
  downloadUrl: string;
}

export function fetchMedia(mediaId: number, signal?: AbortSignal) {
  return kyInstance.get(`/api/v1/media/${mediaId}`, { signal }).json<MediaResponse>();
}

export const mediaQuery = (mediaId: number) =>
  queryOptions({
    queryKey: ['media', mediaId],
    queryFn: ({ signal }) => fetchMedia(mediaId, signal),
    retry: false,
  });
