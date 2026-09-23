import { TextDecoder, TextEncoder } from 'node:util';

import '@testing-library/jest-dom';

Object.assign(globalThis, { TextEncoder, TextDecoder });

process.env.API_ORIGIN = 'http://localhost';

/**
 * jsdom은 <dialog>의 showModal/close를 구현하지 않는다. 모달이 열리고 닫히는 것만 흉내 내
 * `open` 속성과 `close`/`cancel` 이벤트가 실제 브라우저처럼 동작하게 한다.
 */
if (typeof HTMLDialogElement !== 'undefined' && !HTMLDialogElement.prototype.showModal) {
  HTMLDialogElement.prototype.showModal = function showModal(this: HTMLDialogElement) {
    this.open = true;
  };
  HTMLDialogElement.prototype.show = function show(this: HTMLDialogElement) {
    this.open = true;
  };
  HTMLDialogElement.prototype.close = function close(this: HTMLDialogElement, returnValue?: string) {
    if (!this.open) return;
    this.open = false;
    if (returnValue !== undefined) this.returnValue = returnValue;
    this.dispatchEvent(new Event('close'));
  };
}
