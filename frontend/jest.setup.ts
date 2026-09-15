import { TextDecoder, TextEncoder } from 'node:util';

import '@testing-library/jest-dom';

Object.assign(globalThis, { TextEncoder, TextDecoder });

process.env.API_ORIGIN = 'http://localhost';
