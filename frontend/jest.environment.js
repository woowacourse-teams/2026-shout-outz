import { TestEnvironment } from 'jest-environment-jsdom';

export default class extends TestEnvironment {
  async setup() {
    await super.setup();
    for (const key of [
      'AbortSignal',
      'AbortController',
      'fetch',
      'Request',
      'Response',
      'Headers',
      'ReadableStream',
      'WritableStream',
      'TransformStream',
      'BroadcastChannel',
      'TextEncoder',
      'TextDecoder',
    ]) {
      this.global[key] = globalThis[key];
    }
  }
}
