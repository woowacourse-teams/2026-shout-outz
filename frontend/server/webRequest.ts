/// <reference types="node" />
import type express from 'express';

// express 요청 -> Tanstack Router가 해석할 수 있는 웹 표준 Request
export function toWebRequest(req: express.Request, res: express.Response): Request {
  const controller = new AbortController();
  res.on('close', () => {
    if (!res.writableFinished) controller.abort();
  });

  const headers = new Headers();
  for (const [key, value] of Object.entries(req.headers)) {
    if (Array.isArray(value)) value.forEach((item) => headers.append(key, item));
    else if (value !== undefined) headers.set(key, value);
  }

  return new Request(new URL(req.originalUrl, `${req.protocol}://${req.get('host')}`), {
    headers,
    signal: controller.signal,
  });
}
