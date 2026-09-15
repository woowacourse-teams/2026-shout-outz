import { TestEnvironment } from 'jest-environment-jsdom';

// MSW와 ky가 사용하는 표준 네트워크 API를 Node에서 가져온다.
export default class NetworkEnvironment extends TestEnvironment {
  async setup() {
    await super.setup();
    for (const name of [
      'fetch',
      'Headers',
      'Request',
      'Response',
      'FormData',
      'Blob',
      'ReadableStream',
      'WritableStream',
      'TransformStream',
      'BroadcastChannel',
      'AbortController',
      'AbortSignal',
    ]) {
      this.global[name] = globalThis[name];
    }
  }
}
