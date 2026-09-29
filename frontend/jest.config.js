// SWC가 CommonJS로 실행하는 테스트에서 사용하는 ESM 네트워크/Markdown 의존성.
const esmPackages = [
  'ky',
  'until-async',
  'rettime',
  '@open-draft/deferred-promise',
  'react-markdown',
  'remark-[^/]+',
  'rehype-[^/]+',
  'unified',
  'hast-util-[^/]+',
  'mdast-util-[^/]+',
  'micromark(?:-[^/]+)?',
  'unist-util-[^/]+',
  'vfile(?:-[^/]+)?',
  'devlop',
  'comma-separated-tokens',
  'space-separated-tokens',
  'estree-util-is-identifier-name',
  'decode-named-character-reference',
  'character-entities(?:-[^/]+)?',
  'character-reference-invalid',
  'longest-streak',
  'zwitch',
  'ccount',
  'parse-entities',
  'is-alphanumerical',
  'is-alphabetical',
  'is-decimal',
  'is-hexadecimal',
  'stringify-entities',
  'property-information',
  'html-url-attributes',
  'trim-lines',
  'bail',
  'trough',
  'markdown-table',
  'hastscript',
  'web-namespaces',
  'html-void-elements',
  'parse5',
  'entities',
  'is-plain-obj',
  'escape-string-regexp',
];

/** @type {import('jest').Config} */
export default {
  testEnvironment: '<rootDir>/jest.environment.js',
  testEnvironmentOptions: { customExportConditions: ['node', 'node-addons'] },
  // 모달을 열고 검색해 고르는 흐름은 기본 5초로는 빠듯하다.
  testTimeout: 15_000,
  // jsdom + MSW + userEvent 스위트는 워커당 부하가 커서, 기본값(CPU-1)으로는 서로 밀려
  // 실행할 때마다 다른 스위트가 시간 초과로 떨어진다. 절반만 써서 각 워커에 여유를 준다.
  maxWorkers: '50%',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  transform: {
    '^.+\\.(?:[cm]?j|t)sx?$': [
      '@swc/jest',
      {
        swcrc: false,
        jsc: {
          parser: { syntax: 'typescript', tsx: true },
          transform: { react: { runtime: 'automatic' } },
          target: 'es2022',
        },
      },
    ],
  },
  transformIgnorePatterns: [`/node_modules/(?!(${esmPackages.join('|')})/)`],
};
